package com.example.transaction.service;

import com.example.transaction.config.AppProperties;
import com.example.transaction.dto.TransactionConfirmRequest;
import com.example.transaction.dto.TransactionConfirmResponse;
import com.example.transaction.dto.TransactionInitRequest;
import com.example.transaction.dto.TransactionInitResponse;
import com.example.transaction.dto.TransactionPageResponse;
import com.example.transaction.dto.TransactionStatusResponse;
import com.example.transaction.entity.Transaction;
import com.example.transaction.entity.Wallet;
import com.example.transaction.entity.enums.PaymentType;
import com.example.transaction.entity.enums.TransactionStatus;
import com.example.transaction.exception.InsufficientBalanceException;
import com.example.transaction.exception.InvalidTransactionException;
import com.example.transaction.exception.TransactionNotFoundException;
import com.example.transaction.exception.WalletNotFoundException;
import com.example.transaction.kafka.TransactionEventProducer;
import com.example.transaction.mapper.TransactionMapper;
import com.example.transaction.repository.TransactionRepository;
import com.example.transaction.repository.TransactionSpecification;
import com.example.transaction.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final WalletRepository walletRepository;
    private final WalletService walletService;
    private final FeeCalculator feeCalculator;
    private final InitRequestCache initRequestCache;
    private final TransactionMapper transactionMapper;
    private final TransactionEventProducer eventProducer;

    // ==================== INIT METHODS ====================

    @Transactional(readOnly = true)
    public TransactionInitResponse initDeposit(TransactionInitRequest request) {
        log.info("Init deposit for wallet: {}, amount: {}",
                request.getWalletUid(), request.getAmount());

        return initTransaction(request, PaymentType.DEPOSIT);
    }

    @Transactional(readOnly = true)
    public TransactionInitResponse initWithdrawal(TransactionInitRequest request) {
        log.info("Init withdrawal for wallet: {}, amount: {}",
                request.getWalletUid(), request.getAmount());

        return initTransaction(request, PaymentType.WITHDRAWAL);
    }

    @Transactional(readOnly = true)
    public TransactionInitResponse initTransfer(TransactionInitRequest request) {
        log.info("Init transfer from wallet: {} to wallet: {}, amount: {}",
                request.getWalletUid(), request.getTargetWalletUid(), request.getAmount());

        if (request.getTargetWalletUid() == null) {
            throw new InvalidTransactionException("Target wallet is required for transfer");
        }

        if (request.getWalletUid().equals(request.getTargetWalletUid())) {
            throw new InvalidTransactionException("Cannot transfer to the same wallet");
        }

        return initTransaction(request, PaymentType.TRANSFER);
    }

    private TransactionInitResponse initTransaction(TransactionInitRequest request, PaymentType type) {
        // Validate amount
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidTransactionException("Amount must be positive");
        }

        // Get source wallet
        Wallet wallet = walletService.getWalletEntity(request.getWalletUid());

        // Validate wallet is active
        if (!wallet.isActive()) {
            throw new InvalidTransactionException("Wallet is not active: " + wallet.getUid());
        }

        // Calculate fee and total
        BigDecimal fee = feeCalculator.calculateFee(request.getAmount(), type);
        BigDecimal totalAmount = request.getAmount().add(fee);

        // For withdrawal and transfer - check balance
        BigDecimal available = wallet.getBalance();
        if (type == PaymentType.WITHDRAWAL || type == PaymentType.TRANSFER) {
            if (!wallet.hasSufficientBalance(totalAmount)) {
                throw new InsufficientBalanceException(
                        wallet.getUid(), totalAmount, wallet.getBalance());
            }
            available = wallet.getBalance().subtract(totalAmount);
        }

        // For transfer - validate target wallet
        if (type == PaymentType.TRANSFER) {
            Wallet targetWallet = walletService.getWalletEntity(request.getTargetWalletUid());
            if (!targetWallet.isActive()) {
                throw new InvalidTransactionException("Target wallet is not active: " + targetWallet.getUid());
            }
            // Check same currency
            if (!wallet.getWalletType().getCurrencyCode()
                    .equals(targetWallet.getWalletType().getCurrencyCode())) {
                throw new InvalidTransactionException("Wallets must have the same currency");
            }
        }

        // Create init request
        UUID requestUid = UUID.randomUUID();
        LocalDateTime expiresAt = initRequestCache.calculateExpiresAt();

        InitRequest initRequest = InitRequest.builder()
                .requestUid(requestUid)
                .userUid(wallet.getUserUid())
                .walletUid(wallet.getUid())
                .targetWalletUid(request.getTargetWalletUid())
                .type(type)
                .amount(request.getAmount())
                .fee(fee)
                .totalAmount(totalAmount)
                .paymentMethodId(request.getPaymentMethodId())
                .createdAt(LocalDateTime.now())
                .expiresAt(expiresAt)
                .build();

        // Store in cache
        initRequestCache.put(initRequest);

        log.info("Created init request: {} for {} transaction, expires at: {}",
                requestUid, type, expiresAt);

        String currencyCode = wallet.getWalletType().getCurrencyCode();
        return transactionMapper.toInitResponse(initRequest, currencyCode);
    }

    // ==================== CONFIRM METHODS ====================

    @Transactional
    public TransactionConfirmResponse confirmDeposit(TransactionConfirmRequest request) {
        log.info("Confirm deposit for requestUid: {}", request.getRequestUid());

        return confirmTransaction(request, PaymentType.DEPOSIT);
    }

    @Transactional
    public TransactionConfirmResponse confirmWithdrawal(TransactionConfirmRequest request) {
        log.info("Confirm withdrawal for requestUid: {}", request.getRequestUid());

        return confirmTransaction(request, PaymentType.WITHDRAWAL);
    }

    @Transactional
    public TransactionConfirmResponse confirmTransfer(TransactionConfirmRequest request) {
        log.info("Confirm transfer for requestUid: {}", request.getRequestUid());

        // Get and validate init request
        InitRequest initRequest = initRequestCache.getAndRemove(request.getRequestUid());
        validateConfirmRequest(initRequest, request, PaymentType.TRANSFER);

        // Get wallets with lock
        Wallet sourceWallet = walletService.getWalletForUpdate(initRequest.getWalletUid());
        Wallet targetWallet = walletService.getWalletForUpdate(initRequest.getTargetWalletUid());

        // Validate balances again (may have changed since init)
        BigDecimal totalDebit = initRequest.getTotalAmount();
        if (!sourceWallet.hasSufficientBalance(totalDebit)) {
            throw new InsufficientBalanceException(
                    sourceWallet.getUid(), totalDebit, sourceWallet.getBalance());
        }

        // Perform transfer
        sourceWallet.debit(totalDebit);
        targetWallet.credit(initRequest.getAmount());

        walletRepository.save(sourceWallet);
        walletRepository.save(targetWallet);

        // Create completed transaction
        Transaction transaction = Transaction.builder()
                .userUid(initRequest.getUserUid())
                .wallet(sourceWallet)
                .targetWallet(targetWallet)
                .amount(initRequest.getAmount())
                .type(PaymentType.TRANSFER)
                .status(TransactionStatus.COMPLETED)
                .fee(initRequest.getFee())
                .build();

        Transaction savedTransaction = transactionRepository.save(transaction);

        log.info("Transfer completed: {} from wallet {} to wallet {}, amount: {}",
                savedTransaction.getUid(), sourceWallet.getUid(),
                targetWallet.getUid(), initRequest.getAmount());

        return transactionMapper.toConfirmResponse(savedTransaction);
    }

    private TransactionConfirmResponse confirmTransaction(
            TransactionConfirmRequest request, PaymentType expectedType) {

        // Get and validate init request
        InitRequest initRequest = initRequestCache.getAndRemove(request.getRequestUid());
        validateConfirmRequest(initRequest, request, expectedType);

        // Get wallet with lock
        Wallet wallet = walletService.getWalletForUpdate(initRequest.getWalletUid());

        // For withdrawal - reserve balance (debit immediately)
        if (expectedType == PaymentType.WITHDRAWAL) {
            BigDecimal totalDebit = initRequest.getTotalAmount();
            if (!wallet.hasSufficientBalance(totalDebit)) {
                throw new InsufficientBalanceException(
                        wallet.getUid(), totalDebit, wallet.getBalance());
            }
            wallet.debit(totalDebit);
            walletRepository.save(wallet);
        }

        // Create pending transaction
        Transaction transaction = Transaction.builder()
                .userUid(initRequest.getUserUid())
                .wallet(wallet)
                .amount(initRequest.getAmount())
                .type(expectedType)
                .status(TransactionStatus.PENDING)
                .fee(initRequest.getFee())
                .paymentMethodId(initRequest.getPaymentMethodId())
                .build();

        Transaction savedTransaction = transactionRepository.save(transaction);

        log.info("Created pending {} transaction: {}", expectedType, savedTransaction.getUid());

        // Send Kafka event for async processing
        if (expectedType == PaymentType.DEPOSIT) {
            eventProducer.sendDepositRequested(savedTransaction, wallet);
        } else if (expectedType == PaymentType.WITHDRAWAL) {
            eventProducer.sendWithdrawalRequested(savedTransaction, wallet);
        }

        return transactionMapper.toConfirmResponse(savedTransaction);
    }

    private void validateConfirmRequest(
            InitRequest initRequest,
            TransactionConfirmRequest confirmRequest,
            PaymentType expectedType) {

        if (initRequest.getType() != expectedType) {
            throw new InvalidTransactionException(
                    "Invalid transaction type: expected " + expectedType +
                            ", got " + initRequest.getType());
        }

        // Validate walletUid matches
        if (!initRequest.getWalletUid().equals(confirmRequest.getWalletUid())) {
            throw new InvalidTransactionException("Wallet UID mismatch");
        }

        // Validate amount matches
        if (confirmRequest.getAmount() != null &&
                confirmRequest.getAmount().compareTo(initRequest.getAmount()) != 0) {
            throw new InvalidTransactionException("Amount mismatch");
        }
    }

    // ==================== QUERY METHODS ====================

    @Transactional(readOnly = true)
    public TransactionStatusResponse getTransactionStatus(UUID transactionUid) {
        log.debug("Getting transaction status: {}", transactionUid);

        Transaction transaction = transactionRepository.findByIdWithWallets(transactionUid)
                .orElseThrow(() -> new TransactionNotFoundException(transactionUid));

        String currencyCode = transaction.getWallet().getWalletType().getCurrencyCode();
        return transactionMapper.toStatusResponse(transaction, currencyCode);
    }

    @Transactional(readOnly = true)
    public TransactionPageResponse searchTransactions(
            UUID userUid,
            UUID walletUid,
            String type,
            String status,
            LocalDateTime dateFrom,
            LocalDateTime dateTo,
            Integer page,
            Integer size) {

        log.debug("Searching transactions: userUid={}, walletUid={}, type={}, status={}",
                userUid, walletUid, type, status);

        // Parse enums
        PaymentType paymentType = type != null ? PaymentType.valueOf(type.toUpperCase()) : null;
        TransactionStatus transactionStatus = status != null ?
                TransactionStatus.valueOf(status.toUpperCase()) : null;

        // Build specification
        Specification<Transaction> spec = TransactionSpecification.buildSpecification(
                userUid, walletUid, paymentType, transactionStatus, dateFrom, dateTo);

        // Execute query
        PageRequest pageRequest = PageRequest.of(
                page != null ? page : 0,
                size != null ? size : 20,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<Transaction> transactionPage = transactionRepository.findAll(spec, pageRequest);

        // Map to response
        List<TransactionStatusResponse> content = transactionPage.getContent().stream()
                .map(t -> {
                    String currencyCode = t.getWallet().getWalletType().getCurrencyCode();
                    return transactionMapper.toStatusResponse(t, currencyCode);
                })
                .collect(Collectors.toList());

        return TransactionPageResponse.builder()
                .content(content)
                .page(transactionPage.getNumber())
                .size(transactionPage.getSize())
                .totalElements(transactionPage.getTotalElements())
                .totalPages(transactionPage.getTotalPages())
                .build();
    }
}