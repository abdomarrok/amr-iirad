package org.marrok.amriirad.model;

import java.time.LocalDateTime;

/** Represents a debtor (المدين / الملزم بالدفع). */
public class Debtor {

    private int id;
    private String fullName;        // الاسم الكامل
    private String idNumber;        // رقم التعريف / السجل التجاري
    private String address;         // العنوان
    private String phone;           // الهاتف
    private String bankAccount;     // الحساب البنكي / الجاري
    private String cnasNumber;      // رقم الضمان الاجتماعي
    private String nifNumber;       // رقم التعريف الجبائي
    private DebtorType debtorType;  // النوع
    private LocalDateTime createdAt;

    public Debtor() {}

    public Debtor(int id, String fullName, String idNumber,
                  String address, String phone, DebtorType debtorType) {
        this.id         = id;
        this.fullName   = fullName;
        this.idNumber   = idNumber;
        this.address    = address;
        this.phone      = phone;
        this.debtorType = debtorType;
    }

    // -------------------------------------------------------------------------
    // Getters & Setters
    // -------------------------------------------------------------------------

    public int getId()                          { return id; }
    public void setId(int id)                   { this.id = id; }

    public String getFullName()                 { return fullName; }
    public void setFullName(String n)           { this.fullName = n; }

    public String getIdNumber()                 { return idNumber; }
    public void setIdNumber(String n)           { this.idNumber = n; }

    public String getAddress()                  { return address; }
    public void setAddress(String a)            { this.address = a; }

    public String getPhone()                    { return phone; }
    public void setPhone(String p)              { this.phone = p; }

    public String getBankAccount()              { return bankAccount; }
    public void setBankAccount(String b)        { this.bankAccount = b; }

    public String getCnasNumber()               { return cnasNumber; }
    public void setCnasNumber(String c)         { this.cnasNumber = c; }

    public String getNifNumber()                { return nifNumber; }
    public void setNifNumber(String n)          { this.nifNumber = n; }

    public DebtorType getDebtorType()           { return debtorType; }
    public void setDebtorType(DebtorType t)     { this.debtorType = t; }

    public LocalDateTime getCreatedAt()                     { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt)       { this.createdAt = createdAt; }

    @Override
    public String toString() { return fullName; }
}
