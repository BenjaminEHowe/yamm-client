package io.yamm.backend;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.UUID;

/**
 * Stores transactions for NewDay (and perhaps other providers!).
 * @author Benjamin Howe
 */
public class TransactionStore {
    private HashMap<String, UUID> providerIdToYAMMId = new HashMap<>();
    private Transaction[] transactionsArray = new Transaction[0];
    private boolean transactionsArrayUpToDate = true;
    private HashMap<UUID, Transaction> transactions = new HashMap<>();
    private ArrayList<Object[]> transactionOrder = new ArrayList<>();
    private boolean transactionOrderUpToDate = true;

    /**
     * Adds a transaction to the transaction store.
     * @param transaction The transaction to be added.
     */
    public void add(Transaction transaction) {
        transactionsArrayUpToDate = false;

        if (transactions.containsKey(transaction.id)) {
            // if we're updating an existing transaction
            Transaction oldTransaction = transactions.get(transaction.id);
            if (transaction.created == oldTransaction.created && transaction.providerId.equals(oldTransaction.providerId)) {
                transactions.put(transaction.id, transaction);
                return; // if the creation date and provider id are the same, no need to update the order
            } else {
                throw new IllegalArgumentException("The creation date or provider ID of a transaction may not be changed!");
            }
        }

        providerIdToYAMMId.put(transaction.providerId, transaction.id);
        transactions.put(transaction.id, transaction);
        transactionOrder.add(new Object[] {transaction.created, transaction.id});
        transactionOrderUpToDate = false;
    }

    /**
     * Returns the first (earliest) transaction.
     * @return The first transaction.
     */
    public Transaction first() {
        return get(0);
    }

    public Transaction get(int index) {
        sortTransactions();
        try {
            //noinspection SuspiciousMethodCalls
            return transactions.get(transactionOrder.get(index)[1]);
        } catch (IndexOutOfBoundsException e) {
            return null;
        }
    }

    /** Returns the transaction with the given provider ID.
     * @param providerId The provider ID of the requested transaction.
     * @return The requested transaction.
     */
    public Transaction get(String providerId) {
        return transactions.get(providerIdToYAMMId.get(providerId));
    }

    /** Returns the transaction with the given UUID.
     * @param id The UUID of the requested transaction.
     * @return The requested transaction.
     */
    public Transaction get(UUID id) {
        return transactions.get(id);
    }

    /**
     * Returns the last (latest) transaction.
     * @return The last transaction.
     */
    public Transaction last() {
        return get(transactions.size() - 1);
    }

    /**
     * Returns the size of the TransactionStore (the number of transactions it holds).
     * @return The size of the TransactionStore (the number of transactions it holds).
     */
    public int size() {
        return transactions.size();
    }

    /**
     * Sorts the transaction order ArrayList. Does nothing if it is already sorted.
     */
    private void sortTransactions() {
        if (!transactionOrderUpToDate) {
            transactionOrder.sort(Comparator.comparing(o -> ((ZonedDateTime) o[0])));
            transactionOrderUpToDate = true;
        }
    }

    /**
     * Returns a (sorted) array of transactions.
     * @return A (sorted) array of transactions.
     */
    public Transaction[] toArray() {
        if (!transactionsArrayUpToDate) {
            sortTransactions();
            transactionsArray = new Transaction[size()];
            for (int i = 0; i < transactionsArray.length; i++) {
                //noinspection SuspiciousMethodCalls
                transactionsArray[i] = transactions.get(transactionOrder.get(i)[1]);
            }
        }
        return transactionsArray;
    }
}
