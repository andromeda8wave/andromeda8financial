package com.financialapp.models;

public class SummaryRow {
    private String category;
    private double plan;
    private double actual;
    private double deviation; // plan - actual

    public SummaryRow(String category, double plan, double actual) {
        this.category = category;
        this.plan = plan;
        this.actual = actual;
        this.deviation = plan - actual;
    }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public double getPlan() { return plan; }
    public void setPlan(double plan) { this.plan = plan; }

    public double getActual() { return actual; }
    public void setActual(double actual) { this.actual = actual; }

    public double getDeviation() { return deviation; }
    public void setDeviation(double deviation) { this.deviation = deviation; }
}