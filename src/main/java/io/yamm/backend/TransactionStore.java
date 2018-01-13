package io.yamm.backend;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.UUID;

public class TransactionStore {
    private HashMap<String, UUID> providerIdToYAMMId = new HashMap<>();
    private Transaction[] transactionsArray = new Transaction[0];
    private boolean transactionsArrayUpToDate = true;
    private HashMap<UUID, Transaction> transactions = new HashMap<>();
    private ArrayList<Object[]> transactionOrder = new ArrayList<>();
    private boolean transactionOrderUpToDate = true;

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

    public Transaction first() {
        sortTransactions();
        //noinspection SuspiciousMethodCalls
        return transactions.get(transactionOrder.get(0)[1]);
    }

    public Transaction get(int index) {
        sortTransactions();
        return transactions.get((UUID) transactionOrder.get(index)[1]);
    }

    public Transaction get(String providerId) {
        return transactions.get(providerIdToYAMMId.get(providerId));
    }

    public Transaction get(UUID id) {
        return transactions.get(id);
    }

    public Transaction last() {
        sortTransactions();
        //noinspection SuspiciousMethodCalls
        return transactions.get(transactionOrder.get(transactions.size() - 1)[1]);
    }

    public int size() {
        return transactions.size();
    }

    private void sortTransactions() {
        if (!transactionOrderUpToDate) {
            transactionOrder.sort(Comparator.comparing(o -> ((ZonedDateTime) o[0])));
            transactionOrderUpToDate = true;
        }
    }

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
