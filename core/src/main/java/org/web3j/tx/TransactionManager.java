package org.web3j.tx;

import java.io.IOException;
import java.math.BigInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.exceptions.TransactionException;
import org.web3j.tx.response.PollingTransactionReceiptProcessor;
import org.web3j.tx.response.TransactionReceiptProcessor;

/**
 * Transaction manager abstraction for executing transactions with Ethereum client via
 * various mechanisms.
 */
public abstract class TransactionManager {

	private static final Logger log = LoggerFactory.getLogger(TransactionManager.class);

    public static final int DEFAULT_POLLING_ATTEMPTS_PER_TX_HASH = 40;
    public static final long DEFAULT_POLLING_FREQUENCY = 100;

    private final TransactionReceiptProcessor transactionReceiptProcessor;

    protected TransactionManager(TransactionReceiptProcessor transactionReceiptProcessor) {
        this.transactionReceiptProcessor = transactionReceiptProcessor;
    }

    protected TransactionManager(Web3j web3j) {
    	log.info("DEFAULT_POLLING_FREQUENCY:" + DEFAULT_POLLING_FREQUENCY);
        this.transactionReceiptProcessor = new PollingTransactionReceiptProcessor(
                web3j, DEFAULT_POLLING_FREQUENCY, DEFAULT_POLLING_ATTEMPTS_PER_TX_HASH);
    }

    protected TransactionManager(Web3j web3j, int attempts, long sleepDuration) {
        this.transactionReceiptProcessor = new PollingTransactionReceiptProcessor(
                web3j, sleepDuration, attempts);
    }

    TransactionReceipt executeTransaction(
            BigInteger gasPrice, BigInteger gasLimit, String to,
            String data, BigInteger value)
            throws IOException, TransactionException {

        EthSendTransaction ethSendTransaction = sendTransaction(
                gasPrice, gasLimit, to, data, value);
        return processResponse(ethSendTransaction);
    }

    public abstract EthSendTransaction sendTransaction(
            BigInteger gasPrice, BigInteger gasLimit, String to,
            String data, BigInteger value)
            throws IOException;

    public abstract String getFromAddress();

    private TransactionReceipt processResponse(EthSendTransaction transactionResponse)
            throws IOException, TransactionException {
        if (transactionResponse.hasError()) {
            throw new RuntimeException("Error processing transaction request: "
                    + transactionResponse.getError().getMessage());
        }

        String transactionHash = transactionResponse.getTransactionHash();

        return transactionReceiptProcessor.waitForTransactionReceipt(transactionHash);
    }


}
