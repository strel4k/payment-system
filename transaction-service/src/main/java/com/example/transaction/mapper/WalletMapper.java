package com.example.transaction.mapper;

import com.example.transaction.dto.WalletResponse;
import com.example.transaction.entity.Wallet;
import org.springframework.stereotype.Component;

import java.time.ZoneOffset;

@Component
public class WalletMapper {

    public WalletResponse toResponse(Wallet wallet) {
        if (wallet == null) {
            return null;
        }

        return WalletResponse.builder()
                .uid(wallet.getUid())
                .userUid(wallet.getUserUid())
                .walletTypeUid(wallet.getWalletType().getUid())
                .name(wallet.getName())
                .status(WalletResponse.StatusEnum.fromValue(wallet.getStatus().name()))
                .balance(wallet.getBalance())
                .currencyCode(wallet.getWalletType().getCurrencyCode())
                .createdAt(wallet.getCreatedAt() != null
                        ? wallet.getCreatedAt().atOffset(ZoneOffset.UTC)
                        : null)
                .modifiedAt(wallet.getModifiedAt() != null
                        ? wallet.getModifiedAt().atOffset(ZoneOffset.UTC)
                        : null)
                .build();
    }
}