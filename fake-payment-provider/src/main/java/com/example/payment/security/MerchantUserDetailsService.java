package com.example.payment.security;

import com.example.payment.entity.Merchant;
import com.example.payment.repository.MerchantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MerchantUserDetailsService implements UserDetailsService {

    private final MerchantRepository merchantRepository;

    @Override
    public UserDetails loadUserByUsername(String merchantId) throws UsernameNotFoundException {
        Merchant merchant = merchantRepository.findByMerchantId(merchantId)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Merchant not found: " + merchantId));

        return new MerchantPrincipal(merchant);
    }
}