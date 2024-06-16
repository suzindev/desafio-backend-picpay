package com.suzintech.picpay.service;

import com.suzintech.picpay.controller.dto.CreateWalletDTO;
import com.suzintech.picpay.entity.Wallet;
import com.suzintech.picpay.exception.WalletDataAlreadyExistsException;
import com.suzintech.picpay.repository.WalletRepository;
import org.springframework.stereotype.Service;

@Service
public class WalletService {

    private final WalletRepository walletRepository;

    public WalletService(WalletRepository walletRepository) {
        this.walletRepository = walletRepository;
    }

    public Wallet createWallet(CreateWalletDTO dto) {
        var wallet = walletRepository.findByCpfCnpjOrEmail(dto.cpfCnpj(), dto.email());

        if (wallet.isPresent()) {
            throw new WalletDataAlreadyExistsException("CpfCnpj or Email already exists");
        }

        return walletRepository.save(dto.toWallet());
    }
}
