package com.wolfcode.MikrotikHotspot.controller;

import com.wolfcode.MikrotikHotspot.dto.UserCredentials;
import com.wolfcode.MikrotikHotspot.dto.payment.*;
import com.wolfcode.MikrotikHotspot.entity.PaymentSession;
import com.wolfcode.MikrotikHotspot.entity.TransactionsInfo;
import com.wolfcode.MikrotikHotspot.repository.PaymentSessionRepository;
import com.wolfcode.MikrotikHotspot.repository.TransactionRepository;
import com.wolfcode.MikrotikHotspot.service.DarajaService;
import com.wolfcode.MikrotikHotspot.service.MikrotikService;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@Transactional
@RequiredArgsConstructor
@RequestMapping("/payment")
@Slf4j
@CrossOrigin(origins = "*")
public class PaymentController {

    private final DarajaService darajaService;
    private final AcknowledgeResponse acknowledgeResponse;
    private final TikController tikController;
    private final TransactionRepository transactionRepository;
    private final PaymentSessionRepository paymentSessionRepository;
    private final MikrotikService mikrotikService;

    @GetMapping("/token")
    public ResponseEntity<TokenResponse> getAccessToken() {
        return ResponseEntity.ok(darajaService.getAccessToken());
    }


    @PostMapping("/stk-push")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<StkPushSyncResponse> performStkPushTransaction(@Valid @RequestBody PaymentRequest paymentRequest) {

        String tempRequestId = UUID.randomUUID().toString();
        PaymentSession paymentSession = PaymentSession.builder()
                .tempRequestId(tempRequestId)
                .ip(paymentRequest.getIpAddress())
                .mac(paymentRequest.getMacAddress())
                .packageType(paymentRequest.getPackageType())
                .routerName(paymentRequest.getRouterName())
                .build();
        paymentSessionRepository.save(paymentSession);
        System.out.println(paymentSession);

        try {
            StkPushSyncResponse stkResponse = darajaService.
                    performStkPushTransaction(paymentRequest);

            paymentSession.setCheckoutRequestID(stkResponse.getCheckoutRequestID());
            paymentSessionRepository.save(paymentSession);

            return ResponseEntity.ok(stkResponse);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }


    }

    @PostMapping("/stk-query/{checkoutRequestId}")
    public ResponseEntity<?> checkStkPushStatus(@PathVariable String checkoutRequestId) {
        StkQueryResponse stkQueryResponse = darajaService.checkStkPushStatus(checkoutRequestId);

        if (stkQueryResponse != null) {
            log.info("Response Code: {}, Description: {}", stkQueryResponse.getResultCode(), stkQueryResponse.getResultDesc());
        }

        if (stkQueryResponse != null && "0".equals(stkQueryResponse.getResultCode())) {
            log.info("STK Query successful: {}", stkQueryResponse);

            return ResponseEntity.ok(stkQueryResponse);
        } else {
            log.error("STK Query failed or returned invalid response.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Failed to query STK Push transaction.");
        }
    }

    @PostMapping("/transaction-status")
    public ResponseEntity<AcknowledgeResponse> acknowledgeStkPushResponse(@RequestBody StkPushAsyncResponse stkPushAsyncResponse) {

        StkCallback stkCallback = stkPushAsyncResponse.getBody().getStkCallback();

        if (stkCallback.getResultCode() == 0) {
            log.info("Payment successful");

            String mpesaReceiptNumber = "N/A";
            String amount = "N/A";
            String phoneNumber = "N/A";

            for (ItemItem item : stkCallback.getCallbackMetadata().getItem()) {
                if ("MpesaReceiptNumber".equals(item.getName())) {
                    mpesaReceiptNumber = item.getValue() != null ? item.getValue() : "N/A";
                } else if ("Amount".equals(item.getName())) {
                    amount = item.getValue() != null ? item.getValue() : "N/A";
                } else if ("PhoneNumber".equals(item.getName())) {
                    phoneNumber = item.getValue() != null ? item.getValue() : "N/A";
                }
            }
            TransactionsInfo transactionsInfo = TransactionsInfo.builder()
                    .code(mpesaReceiptNumber)
                    .phoneNumber(phoneNumber)
                    .amount(amount)
                    .date(LocalDateTime.now())
                    .build();
            transactionRepository.save(transactionsInfo);

            String checkoutRequestID = stkCallback.getCheckoutRequestID();
            PaymentSession paymentSession = paymentSessionRepository.findByCheckoutRequestID(checkoutRequestID);

            tikController.connectUser(checkoutRequestID, paymentSession.getIp(), paymentSession.getMac(), paymentSession.getPackageType(),
                    paymentSession.getRouterName(), phoneNumber, mpesaReceiptNumber, amount);


            log.info("Transaction Details -> Phone: {}, Amount: {}, Mpesa Receipt: {}", phoneNumber, amount, mpesaReceiptNumber);

        } else {
            log.warn("Payment failed with result code: {}", stkCallback.getResultCode());
        }

        return ResponseEntity.ok(acknowledgeResponse);
    }

    @GetMapping("/userDetails")
    @ResponseStatus(HttpStatus.OK)
    public UserCredentials getUserDetails(@RequestParam String checkoutRequestID) {
        try {
            return mikrotikService.getUserCredentials(checkoutRequestID);
        }catch (Exception e) {
            throw new  IllegalArgumentException("Failed to verify your payment");
        }
    }

}