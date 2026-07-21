package com.oryno.piggy_ledger.data

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object BackupHelper {

    fun escapeCsv(value: Any?): String {
        if (value == null) return ""
        val str = value.toString()
        if (str.contains(",") || str.contains("\"") || str.contains("\n") || str.contains("\r")) {
            return "\"" + str.replace("\"", "\"\"") + "\""
        }
        return str
    }

    fun parseCsvContent(content: String): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        val currentRow = mutableListOf<String>()
        val currentField = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < content.length) {
            val c = content[i]
            if (c == '"') {
                if (inQuotes && i + 1 < content.length && content[i + 1] == '"') {
                    currentField.append('"')
                    i++
                } else {
                    inQuotes = !inQuotes
                }
            } else if (c == ',' && !inQuotes) {
                currentRow.add(currentField.toString())
                currentField.setLength(0)
            } else if ((c == '\n' || c == '\r') && !inQuotes) {
                if (c == '\r' && i + 1 < content.length && content[i + 1] == '\n') {
                    i++
                }
                currentRow.add(currentField.toString())
                currentField.setLength(0)
                if (currentRow.isNotEmpty() && currentRow.any { it.isNotBlank() }) {
                    rows.add(currentRow.toList())
                }
                currentRow.clear()
            } else {
                currentField.append(c)
            }
            i++
        }
        if (currentField.isNotEmpty() || currentRow.isNotEmpty()) {
            currentRow.add(currentField.toString())
            if (currentRow.any { it.isNotBlank() }) {
                rows.add(currentRow.toList())
            }
        }
        return rows
    }

    fun generateFullCsv(
        goals: List<Goal>,
        transactions: List<Transaction>,
        loans: List<Loan>,
        loanPayments: List<LoanPayment>,
        accounts: List<Account>,
        accountTransactions: List<AccountTransaction>,
        pendingTransactions: List<PendingTransaction>,
        streakDates: Set<String>
    ): String {
        val sb = java.lang.StringBuilder()
        
        // Header
        sb.append("RECORD_TYPE,FIELD_1,FIELD_2,FIELD_3,FIELD_4,FIELD_5,FIELD_6,FIELD_7,FIELD_8,FIELD_9,FIELD_10,FIELD_11,FIELD_12,FIELD_13,FIELD_14,FIELD_15,FIELD_16,FIELD_17,FIELD_18,FIELD_19\n")
        
        // 1. Goals
        for (g in goals) {
            sb.append("GOAL,")
                .append(escapeCsv(g.id)).append(",")
                .append(escapeCsv(g.name)).append(",")
                .append(escapeCsv(g.targetAmount)).append(",")
                .append(escapeCsv(g.createdAt))
                .append("\n")
        }
        
        // 2. Transactions
        for (t in transactions) {
            sb.append("TRANSACTION,")
                .append(escapeCsv(t.id)).append(",")
                .append(escapeCsv(t.goalId)).append(",")
                .append(escapeCsv(t.amount)).append(",")
                .append(escapeCsv(t.note)).append(",")
                .append(escapeCsv(t.timestamp)).append(",")
                .append(escapeCsv(t.deadline ?: ""))
                .append("\n")
        }
        
        // 3. Loans
        for (l in loans) {
            sb.append("LOAN,")
                .append(escapeCsv(l.id)).append(",")
                .append(escapeCsv(l.type.name)).append(",")
                .append(escapeCsv(l.amount)).append(",")
                .append(escapeCsv(l.contactName)).append(",")
                .append(escapeCsv(l.phone ?: "")).append(",")
                .append(escapeCsv(l.email ?: "")).append(",")
                .append(escapeCsv(l.photoUri ?: "")).append(",")
                .append(escapeCsv(l.social ?: "")).append(",")
                .append(escapeCsv(l.note)).append(",")
                .append(escapeCsv(l.isPaidOff)).append(",")
                .append(escapeCsv(l.timestamp)).append(",")
                .append(escapeCsv(l.deadline ?: ""))
                .append("\n")
        }

        // 3.1 Loan Payments
        for (lp in loanPayments) {
            sb.append("LOAN_PAYMENT,")
                .append(escapeCsv(lp.id)).append(",")
                .append(escapeCsv(lp.loanId)).append(",")
                .append(escapeCsv(lp.amount)).append(",")
                .append(escapeCsv(lp.timestamp)).append(",")
                .append(escapeCsv(lp.note ?: ""))
                .append("\n")
        }
        
        // 4. Accounts
        for (a in accounts) {
            sb.append("ACCOUNT,")
                .append(escapeCsv(a.id)).append(",")
                .append(escapeCsv(a.name)).append(",")
                .append(escapeCsv(a.type.name)).append(",")
                .append(escapeCsv(a.icon_color)).append(",")
                .append(escapeCsv(a.icon_name)).append(",")
                .append(escapeCsv(a.logo_url ?: "")).append(",")
                .append(escapeCsv(a.local_logo_path ?: "")).append(",")
                .append(escapeCsv(a.currency)).append(",")
                .append(escapeCsv(a.starting_balance)).append(",")
                .append(escapeCsv(a.current_balance)).append(",")
                .append(escapeCsv(a.exclude_from_all)).append(",")
                .append(escapeCsv(a.credit_limit ?: "")).append(",")
                .append(escapeCsv(a.available_credit ?: "")).append(",")
                .append(escapeCsv(a.payment_due_day ?: "")).append(",")
                .append(escapeCsv(a.card_numbers ?: "")).append(",")
                .append(escapeCsv(a.bank_account_no ?: "")).append(",")
                .append(escapeCsv(a.provider ?: "")).append(",")
                .append(escapeCsv(a.insta_pay_fee)).append(",")
                .append(escapeCsv(a.label ?: ""))
                .append("\n")
        }
        
        // 5. Account Transactions
        for (at in accountTransactions) {
            sb.append("ACCOUNT_TRANSACTION,")
                .append(escapeCsv(at.id)).append(",")
                .append(escapeCsv(at.account_id)).append(",")
                .append(escapeCsv(at.amount)).append(",")
                .append(escapeCsv(at.merchant)).append(",")
                .append(escapeCsv(at.timestamp)).append(",")
                .append(escapeCsv(at.source))
                .append("\n")
        }
        
        // 6. Pending Transactions
        for (pt in pendingTransactions) {
            sb.append("PENDING_TRANSACTION,")
                .append(escapeCsv(pt.id)).append(",")
                .append(escapeCsv(pt.amount)).append(",")
                .append(escapeCsv(pt.merchant)).append(",")
                .append(escapeCsv(pt.raw_sms_body)).append(",")
                .append(escapeCsv(pt.sender)).append(",")
                .append(escapeCsv(pt.timestamp))
                .append("\n")
        }
        
        // 7. Streak Dates
        for (sd in streakDates) {
            sb.append("STREAK_DATE,")
                .append(escapeCsv(sd))
                .append("\n")
        }
        
        return sb.toString()
    }

    fun parseFullCsv(csvString: String): FullBackupData {
        val goals = mutableListOf<Goal>()
        val transactions = mutableListOf<Transaction>()
        val loans = mutableListOf<Loan>()
        val loanPayments = mutableListOf<LoanPayment>()
        val accounts = mutableListOf<Account>()
        val accountTransactions = mutableListOf<AccountTransaction>()
        val pendingTransactions = mutableListOf<PendingTransaction>()
        val streakDates = mutableSetOf<String>()
        
        val rows = parseCsvContent(csvString)
        for (row in rows) {
            if (row.isEmpty()) continue
            val type = row[0]
            try {
                when (type) {
                    "GOAL" -> {
                        if (row.size >= 4) {
                            goals.add(
                                Goal(
                                    id = row[1],
                                    name = row[2],
                                    targetAmount = row[3].toDoubleOrNull() ?: 0.0,
                                    createdAt = row[4].toLongOrNull() ?: System.currentTimeMillis()
                                )
                            )
                        }
                    }
                    "TRANSACTION" -> {
                        if (row.size >= 6) {
                            transactions.add(
                                Transaction(
                                    id = row[1],
                                    goalId = row[2],
                                    amount = row[3].toDoubleOrNull() ?: 0.0,
                                    note = row[4],
                                    timestamp = row[5].toLongOrNull() ?: System.currentTimeMillis(),
                                    deadline = row.getOrNull(6)?.toLongOrNull()
                                )
                            )
                        }
                    }
                    "LOAN" -> {
                        if (row.size >= 12) {
                            loans.add(
                                Loan(
                                    id = row[1],
                                    type = if (row[2] == "LENT") LoanType.LENT else LoanType.BORROWED,
                                    amount = row[3].toDoubleOrNull() ?: 0.0,
                                    contactName = row[4],
                                    phone = row[5].takeIf { it.isNotEmpty() },
                                    email = row[6].takeIf { it.isNotEmpty() },
                                    photoUri = row[7].takeIf { it.isNotEmpty() },
                                    social = row[8].takeIf { it.isNotEmpty() },
                                    note = row[9],
                                    isPaidOff = row[10].toBooleanStrictOrNull() ?: false,
                                    timestamp = row[11].toLongOrNull() ?: System.currentTimeMillis(),
                                    deadline = row.getOrNull(12)?.toLongOrNull()
                                )
                            )
                        }
                    }
                    "LOAN_PAYMENT" -> {
                        if (row.size >= 5) {
                            loanPayments.add(
                                LoanPayment(
                                    id = row[1].toLongOrNull() ?: 0L,
                                    loanId = row[2],
                                    amount = row[3].toDoubleOrNull() ?: 0.0,
                                    timestamp = row[4].toLongOrNull() ?: System.currentTimeMillis(),
                                    note = row.getOrNull(5)?.takeIf { it.isNotEmpty() }
                                )
                            )
                        }
                    }
                    "ACCOUNT" -> {
                        if (row.size >= 19) {
                            accounts.add(
                                Account(
                                    id = row[1].toLongOrNull() ?: 0L,
                                    name = row[2],
                                    type = AccountType.valueOf(row[3]),
                                    icon_color = row[4],
                                    icon_name = row[5],
                                    logo_url = row[6].takeIf { it.isNotEmpty() },
                                    local_logo_path = row[7].takeIf { it.isNotEmpty() },
                                    currency = row[8],
                                    starting_balance = row[9].toDoubleOrNull() ?: 0.0,
                                    current_balance = row[10].toDoubleOrNull() ?: 0.0,
                                    exclude_from_all = row[11].toBooleanStrictOrNull() ?: false,
                                    credit_limit = row[12].toDoubleOrNull(),
                                    available_credit = row[13].toDoubleOrNull(),
                                    payment_due_day = row[14].toIntOrNull(),
                                    card_numbers = row[15].takeIf { it.isNotEmpty() },
                                    bank_account_no = row[16].takeIf { it.isNotEmpty() },
                                    provider = row[17].takeIf { it.isNotEmpty() },
                                    insta_pay_fee = row[18].toBooleanStrictOrNull() ?: false,
                                    label = row.getOrNull(19)?.takeIf { it.isNotEmpty() }
                                )
                            )
                        }
                    }
                    "ACCOUNT_TRANSACTION" -> {
                        if (row.size >= 7) {
                            accountTransactions.add(
                                AccountTransaction(
                                    id = row[1].toLongOrNull() ?: 0L,
                                    account_id = row[2].toLongOrNull() ?: 0L,
                                    amount = row[3].toDoubleOrNull() ?: 0.0,
                                    merchant = row[4],
                                    timestamp = row[5].toLongOrNull() ?: System.currentTimeMillis(),
                                    source = row[6]
                                )
                            )
                        }
                    }
                    "PENDING_TRANSACTION" -> {
                        if (row.size >= 7) {
                            pendingTransactions.add(
                                PendingTransaction(
                                    id = row[1].toLongOrNull() ?: 0L,
                                    amount = row[2].toDoubleOrNull() ?: 0.0,
                                    merchant = row[3],
                                    raw_sms_body = row[4],
                                    sender = row[5],
                                    timestamp = row[6].toLongOrNull() ?: System.currentTimeMillis()
                                )
                            )
                        }
                    }
                    "STREAK_DATE" -> {
                        if (row.size >= 2) {
                            streakDates.add(row[1])
                        }
                    }
                }
            } catch (e: Exception) {
                // Ignore corrupt rows gracefully
            }
        }
        
        return FullBackupData(
            goals = goals,
            transactions = transactions,
            loans = loans,
            loanPayments = loanPayments,
            accounts = accounts,
            accountTransactions = accountTransactions,
            pendingTransactions = pendingTransactions,
            streakDates = streakDates
        )
    }

    private fun escapeXml(value: Any?): String {
        if (value == null) return ""
        val str = value.toString()
        return str.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }

    fun generateBeautifulExcel(
        goals: List<Goal>,
        transactions: List<Transaction>,
        loans: List<Loan>,
        loanPayments: List<LoanPayment>,
        accounts: List<Account>,
        accountTransactions: List<AccountTransaction>,
        pendingTransactions: List<PendingTransaction>,
        streakDates: Set<String>
    ): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        
        val totalAssets = accounts.filter { !it.exclude_from_all }.sumOf { it.current_balance }
        val lentTotal = loans.filter { it.type == LoanType.LENT && !it.isPaidOff }.sumOf { it.amount }
        val borrowedTotal = loans.filter { it.type == LoanType.BORROWED && !it.isPaidOff }.sumOf { it.amount }
        val netWorth = totalAssets + lentTotal - borrowedTotal

        val sb = StringBuilder()
        sb.append("""<?xml version="1.0"?>
<?mso-application progid="Excel.Sheet"?>
<Workbook xmlns="urn:schemas-microsoft-com:office:spreadsheet"
 xmlns:o="urn:schemas-microsoft-com:office:office"
 xmlns:x="urn:schemas-microsoft-com:office:excel"
 xmlns:ss="urn:schemas-microsoft-com:office:spreadsheet"
 xmlns:html="http://www.w3.org/TR/REC-html40">
 <Styles>
   <Style ss:ID="Default" ss:Name="Normal">
     <Alignment ss:Vertical="Bottom"/>
     <Borders/>
     <Font ss:FontName="Segoe UI" x:CharSet="1" x:Family="Swiss" ss:Size="11" ss:Color="#1E293B"/>
     <Interior/>
     <NumberFormat/>
     <Protection/>
   </Style>
   <Style ss:ID="Title">
     <Font ss:FontName="Segoe UI" ss:Size="18" ss:Bold="1" ss:Color="#1E293B"/>
     <Alignment ss:Vertical="Center"/>
   </Style>
   <Style ss:ID="Subtitle">
     <Font ss:FontName="Segoe UI" ss:Size="11" ss:Italic="1" ss:Color="#64748B"/>
   </Style>
   <Style ss:ID="HeaderPink">
     <Font ss:FontName="Segoe UI" ss:Size="11" ss:Bold="1" ss:Color="#FFFFFF"/>
     <Interior ss:Color="#E91E63" ss:Pattern="Solid"/>
     <Alignment ss:Horizontal="Center" ss:Vertical="Center"/>
     <Borders>
       <Border ss:Position="Bottom" ss:LineStyle="Continuous" ss:Weight="1" ss:Color="#CCCCCC"/>
     </Borders>
   </Style>
   <Style ss:ID="HeaderNavy">
     <Font ss:FontName="Segoe UI" ss:Size="11" ss:Bold="1" ss:Color="#FFFFFF"/>
     <Interior ss:Color="#1E293B" ss:Pattern="Solid"/>
     <Alignment ss:Horizontal="Center" ss:Vertical="Center"/>
     <Borders>
       <Border ss:Position="Bottom" ss:LineStyle="Continuous" ss:Weight="1" ss:Color="#CCCCCC"/>
     </Borders>
   </Style>
   <Style ss:ID="CardHeader">
     <Font ss:FontName="Segoe UI" ss:Size="12" ss:Bold="1" ss:Color="#1E293B"/>
     <Interior ss:Color="#F1F5F9" ss:Pattern="Solid"/>
     <Borders>
       <Border ss:Position="Bottom" ss:LineStyle="Continuous" ss:Weight="1" ss:Color="#E2E8F0"/>
       <Border ss:Position="Top" ss:LineStyle="Continuous" ss:Weight="1" ss:Color="#E2E8F0"/>
     </Borders>
   </Style>
   <Style ss:ID="DataCell">
     <Borders>
       <Border ss:Position="Bottom" ss:LineStyle="Continuous" ss:Weight="1" ss:Color="#E2E8F0"/>
       <Border ss:Position="Left" ss:LineStyle="Continuous" ss:Weight="1" ss:Color="#E2E8F0"/>
       <Border ss:Position="Right" ss:LineStyle="Continuous" ss:Weight="1" ss:Color="#E2E8F0"/>
       <Border ss:Position="Top" ss:LineStyle="Continuous" ss:Weight="1" ss:Color="#E2E8F0"/>
     </Borders>
   </Style>
   <Style ss:ID="DataCellBold">
     <Font ss:FontName="Segoe UI" ss:Size="11" ss:Bold="1" ss:Color="#1E293B"/>
     <Borders>
       <Border ss:Position="Bottom" ss:LineStyle="Continuous" ss:Weight="1" ss:Color="#E2E8F0"/>
       <Border ss:Position="Left" ss:LineStyle="Continuous" ss:Weight="1" ss:Color="#E2E8F0"/>
       <Border ss:Position="Right" ss:LineStyle="Continuous" ss:Weight="1" ss:Color="#E2E8F0"/>
       <Border ss:Position="Top" ss:LineStyle="Continuous" ss:Weight="1" ss:Color="#E2E8F0"/>
     </Borders>
   </Style>
   <Style ss:ID="DataCellNumber">
     <NumberFormat ss:Format="#,##0.00"/>
     <Borders>
       <Border ss:Position="Bottom" ss:LineStyle="Continuous" ss:Weight="1" ss:Color="#E2E8F0"/>
       <Border ss:Position="Left" ss:LineStyle="Continuous" ss:Weight="1" ss:Color="#E2E8F0"/>
       <Border ss:Position="Right" ss:LineStyle="Continuous" ss:Weight="1" ss:Color="#E2E8F0"/>
       <Border ss:Position="Top" ss:LineStyle="Continuous" ss:Weight="1" ss:Color="#E2E8F0"/>
     </Borders>
   </Style>
 </Styles>
""")

        // SHEET 1: Summary Dashboard
        sb.append(""" <Worksheet ss:Name="Financial Summary">
   <Table ss:ExpandedColumnCount="5" ss:ExpandedRowCount="18" x:FullColumns="1" x:FullRows="1" ss:DefaultRowHeight="20">
     <Column ss:Width="200"/>
     <Column ss:Width="150"/>
     <Column ss:Width="40"/>
     <Column ss:Width="150"/>
     <Column ss:Width="120"/>
     <Row ss:Height="30">
       <Cell><Data ss:Type="String" ss:StyleID="Title">Piggy Ledger Dashboard</Data></Cell>
     </Row>
     <Row ss:Height="20">
       <Cell><Data ss:Type="String" ss:StyleID="Subtitle">Automated financial snapshot exported on ${dateFormat.format(Date())}</Data></Cell>
     </Row>
     <Row ss:Height="15"/>
     <Row ss:Height="25">
       <Cell ss:StyleID="CardHeader"><Data ss:Type="String">Metric Description</Data></Cell>
       <Cell ss:StyleID="CardHeader"><Data ss:Type="String">Value</Data></Cell>
       <Cell/>
       <Cell ss:StyleID="CardHeader"><Data ss:Type="String">Quick Stats</Data></Cell>
       <Cell ss:StyleID="CardHeader"><Data ss:Type="String">Count</Data></Cell>
     </Row>
     <Row>
       <Cell ss:StyleID="DataCell"><Data ss:Type="String">Total Liquid Assets (Accounts)</Data></Cell>
       <Cell ss:StyleID="DataCellNumber"><Data ss:Type="Number">$totalAssets</Data></Cell>
       <Cell/>
       <Cell ss:StyleID="DataCell"><Data ss:Type="String">Total Accounts Registered</Data></Cell>
       <Cell ss:StyleID="DataCellBold"><Data ss:Type="Number">${accounts.size}</Data></Cell>
     </Row>
     <Row>
       <Cell ss:StyleID="DataCell"><Data ss:Type="String">Total Receivables (Lent to others)</Data></Cell>
       <Cell ss:StyleID="DataCellNumber"><Data ss:Type="Number">$lentTotal</Data></Cell>
       <Cell/>
       <Cell ss:StyleID="DataCell"><Data ss:Type="String">Active Goals Running</Data></Cell>
       <Cell ss:StyleID="DataCellBold"><Data ss:Type="Number">${goals.size}</Data></Cell>
     </Row>
     <Row>
       <Cell ss:StyleID="DataCell"><Data ss:Type="String">Total Payables (Borrowed from others)</Data></Cell>
       <Cell ss:StyleID="DataCellNumber"><Data ss:Type="Number">$borrowedTotal</Data></Cell>
       <Cell/>
       <Cell ss:StyleID="DataCell"><Data ss:Type="String">Active Loans Registered</Data></Cell>
       <Cell ss:StyleID="DataCellBold"><Data ss:Type="Number">${loans.size}</Data></Cell>
     </Row>
     <Row>
       <Cell ss:StyleID="DataCellBold"><Data ss:Type="String">Estimated Net Worth</Data></Cell>
       <Cell ss:StyleID="DataCellNumber"><Data ss:Type="Number">$netWorth</Data></Cell>
       <Cell/>
       <Cell ss:StyleID="DataCell"><Data ss:Type="String">Unresolved SMS in Routing Queue</Data></Cell>
       <Cell ss:StyleID="DataCellBold"><Data ss:Type="Number">${pendingTransactions.size}</Data></Cell>
     </Row>
     <Row>
       <Cell ss:StyleID="DataCell"><Data ss:Type="String">Total Money Saved in Goals</Data></Cell>
       <Cell ss:StyleID="DataCellNumber"><Data ss:Type="Number">${transactions.sumOf { it.amount }}</Data></Cell>
       <Cell/>
       <Cell ss:StyleID="DataCell"><Data ss:Type="String">Streak Active Days</Data></Cell>
       <Cell ss:StyleID="DataCellBold"><Data ss:Type="Number">${streakDates.size}</Data></Cell>
     </Row>
   </Table>
 </Worksheet>
""")

        // SHEET 2: Accounts
        sb.append(""" <Worksheet ss:Name="Accounts">
   <Table x:FullColumns="1" x:FullRows="1" ss:DefaultRowHeight="20">
     <Column ss:Width="50"/>
     <Column ss:Width="160"/>
     <Column ss:Width="100"/>
     <Column ss:Width="60"/>
     <Column ss:Width="120"/>
     <Column ss:Width="120"/>
     <Column ss:Width="80"/>
     <Column ss:Width="120"/>
     <Column ss:Width="120"/>
     <Column ss:Width="80"/>
     <Row ss:Height="25">
       <Cell ss:StyleID="HeaderNavy"><Data ss:Type="String">ID</Data></Cell>
       <Cell ss:StyleID="HeaderNavy"><Data ss:Type="String">Account Name</Data></Cell>
       <Cell ss:StyleID="HeaderNavy"><Data ss:Type="String">Type</Data></Cell>
       <Cell ss:StyleID="HeaderNavy"><Data ss:Type="String">Currency</Data></Cell>
       <Cell ss:StyleID="HeaderNavy"><Data ss:Type="String">Starting Balance</Data></Cell>
       <Cell ss:StyleID="HeaderNavy"><Data ss:Type="String">Current Balance</Data></Cell>
       <Cell ss:StyleID="HeaderNavy"><Data ss:Type="String">Excluded</Data></Cell>
       <Cell ss:StyleID="HeaderNavy"><Data ss:Type="String">Account / Card No</Data></Cell>
       <Cell ss:StyleID="HeaderNavy"><Data ss:Type="String">Provider</Data></Cell>
       <Cell ss:StyleID="HeaderNavy"><Data ss:Type="String">InstaPay Fee</Data></Cell>
     </Row>
""")
        for (a in accounts) {
            sb.append("     <Row>\n")
            sb.append("       <Cell ss:StyleID=\"DataCell\"><Data ss:Type=\"Number\">${a.id}</Data></Cell>\n")
            sb.append("       <Cell ss:StyleID=\"DataCellBold\"><Data ss:Type=\"String\">${escapeXml(a.name)}</Data></Cell>\n")
            sb.append("       <Cell ss:StyleID=\"DataCell\"><Data ss:Type=\"String\">${a.type.name}</Data></Cell>\n")
            sb.append("       <Cell ss:StyleID=\"DataCell\"><Data ss:Type=\"String\">${escapeXml(a.currency)}</Data></Cell>\n")
            sb.append("       <Cell ss:StyleID=\"DataCellNumber\"><Data ss:Type=\"Number\">${a.starting_balance}</Data></Cell>\n")
            sb.append("       <Cell ss:StyleID=\"DataCellNumber\"><Data ss:Type=\"Number\">${a.current_balance}</Data></Cell>\n")
            sb.append("       <Cell ss:StyleID=\"DataCell\"><Data ss:Type=\"String\">${if (a.exclude_from_all) "Yes" else "No"}</Data></Cell>\n")
            sb.append("       <Cell ss:StyleID=\"DataCell\"><Data ss:Type=\"String\">${escapeXml(a.card_numbers ?: a.bank_account_no ?: "-")}</Data></Cell>\n")
            sb.append("       <Cell ss:StyleID=\"DataCell\"><Data ss:Type=\"String\">${escapeXml(a.provider ?: "-")}</Data></Cell>\n")
            sb.append("       <Cell ss:StyleID=\"DataCell\"><Data ss:Type=\"String\">${if (a.insta_pay_fee) "Yes" else "No"}</Data></Cell>\n")
            sb.append("     </Row>\n")
        }
        sb.append("""   </Table>
 </Worksheet>
""")

        // SHEET 3: Account Transactions
        sb.append(""" <Worksheet ss:Name="Account Transactions">
   <Table x:FullColumns="1" x:FullRows="1" ss:DefaultRowHeight="20">
     <Column ss:Width="50"/>
     <Column ss:Width="140"/>
     <Column ss:Width="100"/>
     <Column ss:Width="180"/>
     <Column ss:Width="140"/>
     <Column ss:Width="100"/>
     <Row ss:Height="25">
       <Cell ss:StyleID="HeaderPink"><Data ss:Type="String">ID</Data></Cell>
       <Cell ss:StyleID="HeaderPink"><Data ss:Type="String">Account Name</Data></Cell>
       <Cell ss:StyleID="HeaderPink"><Data ss:Type="String">Amount (EGP)</Data></Cell>
       <Cell ss:StyleID="HeaderPink"><Data ss:Type="String">Merchant / Description</Data></Cell>
       <Cell ss:StyleID="HeaderPink"><Data ss:Type="String">Date &amp; Time</Data></Cell>
       <Cell ss:StyleID="HeaderPink"><Data ss:Type="String">Source</Data></Cell>
     </Row>
""")
        val accountMap = accounts.associateBy { it.id }
        for (at in accountTransactions) {
            val accountName = accountMap[at.account_id]?.name ?: "Account #${at.account_id}"
            sb.append("     <Row>\n")
            sb.append("       <Cell ss:StyleID=\"DataCell\"><Data ss:Type=\"Number\">${at.id}</Data></Cell>\n")
            sb.append("       <Cell ss:StyleID=\"DataCellBold\"><Data ss:Type=\"String\">${escapeXml(accountName)}</Data></Cell>\n")
            sb.append("       <Cell ss:StyleID=\"DataCellNumber\"><Data ss:Type=\"Number\">${at.amount}</Data></Cell>\n")
            sb.append("       <Cell ss:StyleID=\"DataCell\"><Data ss:Type=\"String\">${escapeXml(at.merchant)}</Data></Cell>\n")
            sb.append("       <Cell ss:StyleID=\"DataCell\"><Data ss:Type=\"String\">${dateFormat.format(Date(at.timestamp))}</Data></Cell>\n")
            sb.append("       <Cell ss:StyleID=\"DataCell\"><Data ss:Type=\"String\">${escapeXml(at.source)}</Data></Cell>\n")
            sb.append("     </Row>\n")
        }
        sb.append("""   </Table>
 </Worksheet>
""")

        // SHEET 4: Goals
        sb.append(""" <Worksheet ss:Name="Goals">
   <Table x:FullColumns="1" x:FullRows="1" ss:DefaultRowHeight="20">
     <Column ss:Width="80"/>
     <Column ss:Width="180"/>
     <Column ss:Width="120"/>
     <Column ss:Width="120"/>
     <Column ss:Width="100"/>
     <Column ss:Width="140"/>
     <Row ss:Height="25">
       <Cell ss:StyleID="HeaderNavy"><Data ss:Type="String">ID</Data></Cell>
       <Cell ss:StyleID="HeaderNavy"><Data ss:Type="String">Goal Name</Data></Cell>
       <Cell ss:StyleID="HeaderNavy"><Data ss:Type="String">Target Amount</Data></Cell>
       <Cell ss:StyleID="HeaderNavy"><Data ss:Type="String">Total Saved</Data></Cell>
       <Cell ss:StyleID="HeaderNavy"><Data ss:Type="String">Completion (%)</Data></Cell>
       <Cell ss:StyleID="HeaderNavy"><Data ss:Type="String">Created At</Data></Cell>
     </Row>
""")
        val savedMap = transactions.groupBy { it.goalId }.mapValues { (_, txs) -> txs.sumOf { it.amount } }
        for (g in goals) {
            val totalSaved = savedMap[g.id] ?: 0.0
            val pct = if (g.targetAmount > 0) (totalSaved / g.targetAmount) * 100 else 0.0
            sb.append("     <Row>\n")
            sb.append("       <Cell ss:StyleID=\"DataCell\"><Data ss:Type=\"String\">${escapeXml(g.id)}</Data></Cell>\n")
            sb.append("       <Cell ss:StyleID=\"DataCellBold\"><Data ss:Type=\"String\">${escapeXml(g.name)}</Data></Cell>\n")
            sb.append("       <Cell ss:StyleID=\"DataCellNumber\"><Data ss:Type=\"Number\">${g.targetAmount}</Data></Cell>\n")
            sb.append("       <Cell ss:StyleID=\"DataCellNumber\"><Data ss:Type=\"Number\">$totalSaved</Data></Cell>\n")
            sb.append("       <Cell ss:StyleID=\"DataCellNumber\"><Data ss:Type=\"Number\">$pct</Data></Cell>\n")
            sb.append("       <Cell ss:StyleID=\"DataCell\"><Data ss:Type=\"String\">${dateFormat.format(Date(g.createdAt))}</Data></Cell>\n")
            sb.append("     </Row>\n")
        }
        sb.append("""   </Table>
 </Worksheet>
""")

        // SHEET 5: Goal Transactions
        sb.append(""" <Worksheet ss:Name="Goal Transactions">
   <Table x:FullColumns="1" x:FullRows="1" ss:DefaultRowHeight="20">
     <Column ss:Width="80"/>
     <Column ss:Width="180"/>
     <Column ss:Width="100"/>
     <Column ss:Width="200"/>
     <Column ss:Width="140"/>
     <Column ss:Width="140"/>
     <Row ss:Height="25">
       <Cell ss:StyleID="HeaderPink"><Data ss:Type="String">ID</Data></Cell>
       <Cell ss:StyleID="HeaderPink"><Data ss:Type="String">Goal Name</Data></Cell>
       <Cell ss:StyleID="HeaderPink"><Data ss:Type="String">Amount</Data></Cell>
       <Cell ss:StyleID="HeaderPink"><Data ss:Type="String">Note</Data></Cell>
       <Cell ss:StyleID="HeaderPink"><Data ss:Type="String">Date &amp; Time</Data></Cell>
       <Cell ss:StyleID="HeaderPink"><Data ss:Type="String">Deadline</Data></Cell>
     </Row>
""")
        val goalsMap = goals.associateBy { it.id }
        for (t in transactions) {
            val goalName = goalsMap[t.goalId]?.name ?: "Goal #${t.goalId}"
            sb.append("     <Row>\n")
            sb.append("       <Cell ss:StyleID=\"DataCell\"><Data ss:Type=\"String\">${escapeXml(t.id)}</Data></Cell>\n")
            sb.append("       <Cell ss:StyleID=\"DataCellBold\"><Data ss:Type=\"String\">${escapeXml(goalName)}</Data></Cell>\n")
            sb.append("       <Cell ss:StyleID=\"DataCellNumber\"><Data ss:Type=\"Number\">${t.amount}</Data></Cell>\n")
            sb.append("       <Cell ss:StyleID=\"DataCell\"><Data ss:Type=\"String\">${escapeXml(t.note)}</Data></Cell>\n")
            sb.append("       <Cell ss:StyleID=\"DataCell\"><Data ss:Type=\"String\">${dateFormat.format(Date(t.timestamp))}</Data></Cell>\n")
            sb.append("       <Cell ss:StyleID=\"DataCell\"><Data ss:Type=\"String\">${t.deadline?.let { dateFormat.format(Date(it)) } ?: "-"}</Data></Cell>\n")
            sb.append("     </Row>\n")
        }
        sb.append("""   </Table>
 </Worksheet>
""")

        // SHEET 6: Loans
        sb.append(""" <Worksheet ss:Name="Loans">
   <Table x:FullColumns="1" x:FullRows="1" ss:DefaultRowHeight="20">
     <Column ss:Width="80"/>
     <Column ss:Width="100"/>
     <Column ss:Width="120"/>
     <Column ss:Width="100"/>
     <Column ss:Width="180"/>
     <Column ss:Width="120"/>
     <Column ss:Width="80"/>
     <Column ss:Width="140"/>
     <Column ss:Width="140"/>
     <Row ss:Height="25">
       <Cell ss:StyleID="HeaderNavy"><Data ss:Type="String">ID</Data></Cell>
       <Cell ss:StyleID="HeaderNavy"><Data ss:Type="String">Loan Type</Data></Cell>
       <Cell ss:StyleID="HeaderNavy"><Data ss:Type="String">Contact Name</Data></Cell>
       <Cell ss:StyleID="HeaderNavy"><Data ss:Type="String">Amount</Data></Cell>
       <Cell ss:StyleID="HeaderNavy"><Data ss:Type="String">Note</Data></Cell>
       <Cell ss:StyleID="HeaderNavy"><Data ss:Type="String">Phone/Email/Social</Data></Cell>
       <Cell ss:StyleID="HeaderNavy"><Data ss:Type="String">Paid Off</Data></Cell>
       <Cell ss:StyleID="HeaderNavy"><Data ss:Type="String">Date &amp; Time</Data></Cell>
       <Cell ss:StyleID="HeaderNavy"><Data ss:Type="String">Deadline</Data></Cell>
     </Row>
""")
        for (l in loans) {
            val contactInfo = listOfNotNull(l.phone, l.email, l.social).joinToString(" | ")
            sb.append("     <Row>\n")
            sb.append("       <Cell ss:StyleID=\"DataCell\"><Data ss:Type=\"String\">${escapeXml(l.id)}</Data></Cell>\n")
            sb.append("       <Cell ss:StyleID=\"DataCell\"><Data ss:Type=\"String\">${l.type.name}</Data></Cell>\n")
            sb.append("       <Cell ss:StyleID=\"DataCellBold\"><Data ss:Type=\"String\">${escapeXml(l.contactName)}</Data></Cell>\n")
            sb.append("       <Cell ss:StyleID=\"DataCellNumber\"><Data ss:Type=\"Number\">${l.amount}</Data></Cell>\n")
            sb.append("       <Cell ss:StyleID=\"DataCell\"><Data ss:Type=\"String\">${escapeXml(l.note)}</Data></Cell>\n")
            sb.append("       <Cell ss:StyleID=\"DataCell\"><Data ss:Type=\"String\">${escapeXml(contactInfo.ifEmpty { "-" })}</Data></Cell>\n")
            sb.append("       <Cell ss:StyleID=\"DataCell\"><Data ss:Type=\"String\">${if (l.isPaidOff) "Paid Off" else "Active"}</Data></Cell>\n")
            sb.append("       <Cell ss:StyleID=\"DataCell\"><Data ss:Type=\"String\">${dateFormat.format(Date(l.timestamp))}</Data></Cell>\n")
            sb.append("       <Cell ss:StyleID=\"DataCell\"><Data ss:Type=\"String\">${l.deadline?.let { dateFormat.format(Date(it)) } ?: "-"}</Data></Cell>\n")
            sb.append("     </Row>\n")
        }
        sb.append("""   </Table>
 </Worksheet>
""")

        // SHEET 6.5: Loan Payments
        sb.append(""" <Worksheet ss:Name="Loan Payments">
   <Table x:FullColumns="1" x:FullRows="1" ss:DefaultRowHeight="20">
     <Column ss:Width="80"/>
     <Column ss:Width="100"/>
     <Column ss:Width="120"/>
     <Column ss:Width="180"/>
     <Column ss:Width="140"/>
     <Row ss:Height="25">
       <Cell ss:StyleID="HeaderNavy"><Data ss:Type="String">ID</Data></Cell>
       <Cell ss:StyleID="HeaderNavy"><Data ss:Type="String">Loan ID</Data></Cell>
       <Cell ss:StyleID="HeaderNavy"><Data ss:Type="String">Amount Paid</Data></Cell>
       <Cell ss:StyleID="HeaderNavy"><Data ss:Type="String">Note</Data></Cell>
       <Cell ss:StyleID="HeaderNavy"><Data ss:Type="String">Timestamp</Data></Cell>
     </Row>
""")
        for (lp in loanPayments) {
            sb.append("     <Row>\n")
            sb.append("       <Cell ss:StyleID=\"DataCell\"><Data ss:Type=\"Number\">${lp.id}</Data></Cell>\n")
            sb.append("       <Cell ss:StyleID=\"DataCell\"><Data ss:Type=\"String\">${escapeXml(lp.loanId)}</Data></Cell>\n")
            sb.append("       <Cell ss:StyleID=\"DataCellNumber\"><Data ss:Type=\"Number\">${lp.amount}</Data></Cell>\n")
            sb.append("       <Cell ss:StyleID=\"DataCell\"><Data ss:Type=\"String\">${escapeXml(lp.note ?: "-")}</Data></Cell>\n")
            sb.append("       <Cell ss:StyleID=\"DataCell\"><Data ss:Type=\"String\">${dateFormat.format(Date(lp.timestamp))}</Data></Cell>\n")
            sb.append("     </Row>\n")
        }
        sb.append("""   </Table>
 </Worksheet>
""")

        // SHEET 7: Pending SMS Transactions
        sb.append(""" <Worksheet ss:Name="Pending SMS Queue">
   <Table x:FullColumns="1" x:FullRows="1" ss:DefaultRowHeight="20">
     <Column ss:Width="50"/>
     <Column ss:Width="100"/>
     <Column ss:Width="100"/>
     <Column ss:Width="120"/>
     <Column ss:Width="250"/>
     <Column ss:Width="140"/>
     <Row ss:Height="25">
       <Cell ss:StyleID="HeaderPink"><Data ss:Type="String">ID</Data></Cell>
       <Cell ss:StyleID="HeaderPink"><Data ss:Type="String">Sender</Data></Cell>
       <Cell ss:StyleID="HeaderPink"><Data ss:Type="String">Amount</Data></Cell>
       <Cell ss:StyleID="HeaderPink"><Data ss:Type="String">Extracted Merchant</Data></Cell>
       <Cell ss:StyleID="HeaderPink"><Data ss:Type="String">Raw SMS Body</Data></Cell>
       <Cell ss:StyleID="HeaderPink"><Data ss:Type="String">Received At</Data></Cell>
     </Row>
""")
        for (pt in pendingTransactions) {
            sb.append("     <Row>\n")
            sb.append("       <Cell ss:StyleID=\"DataCell\"><Data ss:Type=\"Number\">${pt.id}</Data></Cell>\n")
            sb.append("       <Cell ss:StyleID=\"DataCellBold\"><Data ss:Type=\"String\">${escapeXml(pt.sender)}</Data></Cell>\n")
            sb.append("       <Cell ss:StyleID=\"DataCellNumber\"><Data ss:Type=\"Number\">${pt.amount}</Data></Cell>\n")
            sb.append("       <Cell ss:StyleID=\"DataCell\"><Data ss:Type=\"String\">${escapeXml(pt.merchant)}</Data></Cell>\n")
            sb.append("       <Cell ss:StyleID=\"DataCell\"><Data ss:Type=\"String\">${escapeXml(pt.raw_sms_body)}</Data></Cell>\n")
            sb.append("       <Cell ss:StyleID=\"DataCell\"><Data ss:Type=\"String\">${dateFormat.format(Date(pt.timestamp))}</Data></Cell>\n")
            sb.append("     </Row>\n")
        }
        sb.append("""   </Table>
 </Worksheet>
""")

        sb.append("</Workbook>\n")
        return sb.toString()
    }
}

data class FullBackupData(
    val goals: List<Goal>,
    val transactions: List<Transaction>,
    val loans: List<Loan>,
    val loanPayments: List<LoanPayment> = emptyList(),
    val accounts: List<Account>,
    val accountTransactions: List<AccountTransaction>,
    val pendingTransactions: List<PendingTransaction>,
    val streakDates: Set<String>
)
