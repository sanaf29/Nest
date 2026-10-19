package com.nest.service.impl;

import com.nest.dto.request.TransferRequest;
import com.nest.dto.response.TransferResponse;
import com.nest.exception.ResourceNotFoundException;
import com.nest.mapper.TransferMapper;
import com.nest.model.FinancialAccount;
import com.nest.model.Transfer;
import com.nest.repository.FinancialAccountRepository;
import com.nest.repository.TransferRepository;
import com.nest.service.TransferService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransferServiceImpl implements TransferService {

    private final TransferRepository transferRepository;
    private final FinancialAccountRepository accountRepository;
    private final TransferMapper transferMapper;

    public TransferServiceImpl(TransferRepository transferRepository,
                               FinancialAccountRepository accountRepository,
                               TransferMapper transferMapper) {
        this.transferRepository = transferRepository;
        this.accountRepository = accountRepository;
        this.transferMapper = transferMapper;
    }

    /**
     * One database transaction: the transfer row and both entries are inserted together (cascade),
     * so a failure part-way through rolls everything back.
     */
    @Override
    @Transactional
    public TransferResponse create(long userId, TransferRequest request) {
        FinancialAccount source = findOwnedAccount(userId, request.sourceAccountId());
        FinancialAccount destination = findOwnedAccount(userId, request.destinationAccountId());

        Transfer transfer = Transfer.between(source, destination, request.amount(),
                request.transferDate(), request.description());
        // Flush so generated entry ids are available for the response.
        return transferMapper.toResponse(transferRepository.saveAndFlush(transfer));
    }

    @Override
    @Transactional(readOnly = true)
    public TransferResponse get(long userId, long transferId) {
        return transferMapper.toResponse(findOwnedTransfer(userId, transferId));
    }

    @Override
    @Transactional
    public void delete(long userId, long transferId) {
        Transfer transfer = findOwnedTransfer(userId, transferId);
        transfer.requireRemovable();
        transferRepository.delete(transfer);
    }

    private FinancialAccount findOwnedAccount(long userId, long accountId) {
        return accountRepository.findByIdAndUserId(accountId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Account", accountId));
    }

    private Transfer findOwnedTransfer(long userId, long transferId) {
        return transferRepository.findOwned(transferId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Transfer", transferId));
    }
}
