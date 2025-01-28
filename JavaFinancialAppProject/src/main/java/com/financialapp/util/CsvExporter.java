package com.financialapp.util;

import com.financialapp.models.Transaction;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class CsvExporter {

    public static void exportTransactions(List<Transaction> transactions, String filePath) {
        try (FileWriter writer = new FileWriter(filePath)) {
            writer.write("ID,Date,CategoryId,SubcategoryId,Amount,CurrencyId,WalletId,Comment\n");
            for (Transaction tx : transactions) {
                writer.write(tx.getId() + "," +
                             tx.getDate() + "," +
                             tx.getCategoryId() + "," +
                             tx.getSubcategoryId() + "," +
                             tx.getAmount() + "," +
                             tx.getCurrencyId() + "," +
                             tx.getWalletId() + "," +
                             (tx.getComment() == null ? "" : tx.getComment().replace(",", " ")) +
                             "\n");
            }
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}