package com.suzintech.picpay.service;

import com.suzintech.picpay.controller.dto.TransferDTO;
import com.suzintech.picpay.entity.Transfer;
import com.suzintech.picpay.entity.Wallet;
import com.suzintech.picpay.exception.InsufficientBalanceException;
import com.suzintech.picpay.exception.TransferNotAllowedForWalletTypeException;
import com.suzintech.picpay.exception.TransferNotAuthorizedException;
import com.suzintech.picpay.exception.WalletNotFoundException;
import com.suzintech.picpay.repository.TransferRepository;
import com.suzintech.picpay.repository.WalletRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.CompletableFuture;

@Service
public class TransferService {

    private final AuthorizationService authorizationService;
    private final NotificationService notificationService;
    private final TransferRepository transferRepository;
    private final WalletRepository walletRepository;

    public TransferService(AuthorizationService authorizationService,
                           NotificationService notificationService,
                           TransferRepository transferRepository,
                           WalletRepository walletRepository) {
        this.authorizationService = authorizationService;
        this.notificationService = notificationService;
        this.transferRepository = transferRepository;
        this.walletRepository = walletRepository;
    }

    @Transactional
    public Transfer transfer(TransferDTO dto) {
        var sender = walletRepository.findById(dto.payer())
                .orElseThrow(() -> new WalletNotFoundException(dto.payer()));

        var receiver = walletRepository.findById(dto.payee())
                .orElseThrow(() -> new WalletNotFoundException(dto.payee()));

        validateTransfer(dto, sender);

        sender.debit(dto.value());
        receiver.credit(dto.value());

        var transfer = new Transfer(sender, receiver, dto.value());

        walletRepository.save(sender);
        walletRepository.save(receiver);
        var result = transferRepository.save(transfer);

        CompletableFuture.runAsync(() -> notificationService.sendNotification(result));

        return result;
    }

    private void validateTransfer(TransferDTO dto, Wallet sender) {
        if (!sender.isTransferAllowedForWalletType()) {
            throw new TransferNotAllowedForWalletTypeException();
        }

        if (!sender.isBalancerEqualOrGreaterThan(dto.value())) {
            throw new InsufficientBalanceException();
        }

        if (!authorizationService.isAuthorized(dto)) {
            throw new TransferNotAuthorizedException();
        }
    }
}
