package com.example.transaction.service;

import com.example.transaction.dto.CreateWalletRequest;
import com.example.transaction.dto.WalletResponse;
import com.example.transaction.entity.Wallet;
import com.example.transaction.entity.WalletType;
import com.example.transaction.entity.enums.WalletStatus;
import com.example.transaction.exception.DuplicateWalletException;
import com.example.transaction.exception.WalletNotFoundException;
import com.example.transaction.exception.WalletTypeNotFoundException;
import com.example.transaction.mapper.WalletMapper;
import com.example.transaction.repository.WalletRepository;
import com.example.transaction.repository.WalletTypeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WalletService {

    private final WalletRepository walletRepository;
    private final WalletTypeRepository walletTypeRepository;
    private final WalletMapper walletMapper;

    @Transactional
    public WalletResponse createWallet(CreateWalletRequest request) {
        log.info("Creating wallet for user: {}, walletType: {}",
                request.getUserUid(), request.getWalletTypeUid());

        UUID userUid = request.getUserUid();
        UUID walletTypeUid = request.getWalletTypeUid();

        // Check if wallet type exists
        WalletType walletType = walletTypeRepository.findById(walletTypeUid)
                .orElseThrow(() -> new WalletTypeNotFoundException(walletTypeUid));

        // Check if user already has wallet of this type
        if (walletRepository.existsByUserUidAndWalletTypeUid(userUid, walletTypeUid)) {
            throw new DuplicateWalletException(userUid, walletTypeUid);
        }

        // Create new wallet
        Wallet wallet = Wallet.builder()
                .name(request.getName())
                .walletType(walletType)
                .userUid(userUid)
                .status(WalletStatus.ACTIVE)
                .balance(BigDecimal.ZERO)
                .build();

        Wallet savedWallet = walletRepository.save(wallet);
        log.info("Created wallet: {} for user: {}", savedWallet.getUid(), userUid);

        return walletMapper.toResponse(savedWallet);
    }

    @Transactional(readOnly = true)
    public WalletResponse getWallet(UUID walletUid) {
        log.debug("Getting wallet: {}", walletUid);

        Wallet wallet = walletRepository.findByIdWithWalletType(walletUid)
                .orElseThrow(() -> new WalletNotFoundException(walletUid));

        return walletMapper.toResponse(wallet);
    }

    @Transactional(readOnly = true)
    public List<WalletResponse> getWalletsByUser(UUID userUid) {
        log.debug("Getting wallets for user: {}", userUid);

        List<Wallet> wallets = walletRepository.findByUserUid(userUid);

        return wallets.stream()
                .map(walletMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Wallet getWalletEntity(UUID walletUid) {
        return walletRepository.findByIdWithWalletType(walletUid)
                .orElseThrow(() -> new WalletNotFoundException(walletUid));
    }

    @Transactional
    public Wallet getWalletForUpdate(UUID walletUid) {
        return walletRepository.findByIdForUpdate(walletUid)
                .orElseThrow(() -> new WalletNotFoundException(walletUid));
    }
}