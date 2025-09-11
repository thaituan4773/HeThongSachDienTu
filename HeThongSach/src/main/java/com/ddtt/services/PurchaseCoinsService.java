package com.ddtt.services;

import com.ddtt.dtos.CoinPackDTO;
import com.ddtt.dtos.PageResponseDTO;
import com.ddtt.dtos.PurchaseCoinsDTO;
import com.ddtt.dtos.PurchaseHistoryDTO;
import com.ddtt.exceptions.NotFoundException;
import com.ddtt.repositories.PurchaseCoinsRepository;
import io.micronaut.core.async.publisher.Publishers;
import jakarta.inject.Singleton;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.reactivestreams.Publisher;

@Singleton
@RequiredArgsConstructor
public class PurchaseCoinsService {

    private final PurchaseCoinsRepository purchaseCoinsRepository;
    private final CoinPackService coinPackService;
    private final MOMOService momoService;

    public Publisher<Map<String, Object>> createTransaction(int accountId, int coinPackId, String paymentMethod)
            throws IOException, NoSuchAlgorithmException, InvalidKeyException {
        CoinPackDTO coinPack = coinPackService.getCoinPackInfo(coinPackId);
        UUID transactionId = purchaseCoinsRepository.createPurchase(accountId, coinPackId, "MOMO");
        return momoService.createPaymentUrl(transactionId, accountId, coinPack.getCoinAmount(), coinPack.getPrice(), coinPack.getCoinPackName());
    }

    public PurchaseCoinsDTO getTransactionById(UUID transactionId) {
        PurchaseCoinsDTO dto = purchaseCoinsRepository.findByTransactionId(transactionId);
        if (dto == null) {
            throw new NotFoundException("Transaction not found: " + transactionId);
        }
        return dto;
    }

    public List<PurchaseCoinsDTO> findByAccountId(int accountId) {
        return purchaseCoinsRepository.findByAccountId(accountId);
    }

    public boolean verifyPayment(Map<String, Object> payload)
            throws InvalidKeyException, NoSuchAlgorithmException {
        if (!momoService.verifyResponseSignature(payload)) {
            throw new IllegalStateException("Chữ ký momo không hợp lệ");
        }

        UUID transactionId = UUID.fromString(String.valueOf(payload.get("orderId")));
        PurchaseCoinsDTO transaction = purchaseCoinsRepository.findByTransactionId(transactionId);
        if (transaction == null) {
            throw new IllegalStateException("Không tìm thấy giao dịch: " + transactionId);
        }

        // Kiểm tra số tiền
        long amountFromMoMo = ((Number) payload.get("amount")).longValue();
        long amountFromDb = transaction.getMoneyAmount();
        if (amountFromDb != amountFromMoMo) {
            throw new IllegalStateException("Thông tin giao dịch không hợp lệ: " + transactionId);
        }

        int resultCode = ((Number) payload.get("resultCode")).intValue();
        String newStatus = (resultCode == 0) ? "SUCCESS" : "FAILED";
        purchaseCoinsRepository.updateStatus(transactionId, newStatus);
        return true;
    }
    
    public void updateStatus(UUID transactionId, String newStatus){
        purchaseCoinsRepository.updateStatus(transactionId, newStatus);
    }

    public String getTransactionStatus(UUID transactionId) {
        var purchase = purchaseCoinsRepository.findByTransactionId(transactionId);
        if (purchase == null) {
            throw new NotFoundException("Không tìm thấy giao dịch");
        }

        return purchase.getStatus();
    }

    public PageResponseDTO<PurchaseHistoryDTO> getPurchaseHistory(int accountId, int page) {
        int size = 50;
        return purchaseCoinsRepository.getPurchaseHistoryPaged(accountId, page, size);
    }
}
