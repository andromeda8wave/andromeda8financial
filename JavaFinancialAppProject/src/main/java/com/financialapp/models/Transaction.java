package com.financialapp.models;

import java.time.LocalDate;

public class Transaction {
    private long id;
    private LocalDate date;
    private long categoryId;
    private long subcategoryId;
    private double amount;
    private long currencyId;
    private long walletId;
    private String comment;

    public Transaction() {}

    public Transaction(long id,
                       LocalDate date,
                       long categoryId,
                       long subcategoryId,
                       double amount,
                       long currencyId,
                       long walletId,
                       String comment) {
        this.id = id;
        this.date = date;
        this.categoryId = categoryId;
        this.subcategoryId = subcategoryId;
        this.amount = amount;
        this.currencyId = currencyId;
        this.walletId = walletId;
        this.comment = comment;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public long getCategoryId() { return categoryId; }
    public void setCategoryId(long categoryId) { this.categoryId = categoryId; }

    public long getSubcategoryId() { return subcategoryId; }
    public void setSubcategoryId(long subcategoryId) { this.subcategoryId = subcategoryId; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public long getCurrencyId() { return currencyId; }
    public void setCurrencyId(long currencyId) { this.currencyId = currencyId; }

    public long getWalletId() { return walletId; }
    public void setWalletId(long walletId) { this.walletId = walletId; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
}