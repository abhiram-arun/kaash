package dev.expensetracker.parse

/** Real-format bank SMS, used by unit tests and the debug "inject samples" action. (sender, body) */
val SAMPLE_SMS = listOf(
    "VM-FEDBNK" to "Debited Rs 77.00 from a/c X8868 on 16Nov26 19:57 via UPI to MANIGANDAN U. Ref 668635377490.Bal Rs 10489.44. Not you?Call 18004251199 -Federal Bank",
    "VM-FEDBNK" to "INR 24.00 spent on your credit card ending with 6612 at 631948987062    anil s on 15-11-2026. Available limit Rs.16,305.87. Not you, call 18004201199 -Federal Bank",
    "AD-CANBNK" to "Dear Customer, Acct XXX437 Dr. INR 27,500.00 on 11/11/26 to myG KOLLAM F; UPI: 668125295496; Bal INR 58,533.06.Not you?SMS BLOCKUPI to 9901771222-CanaraBank",
    "AD-CANBNK" to "CardTxn:A/C XXX437 Dr INR 112.54 on 08/11/26 10:44 to norealNOIDA;RRN:631205681380;Bal INR 86,033.06. If fraud,SMS SUSPECT to 56161/dial 1930-Canara Bank",
    "AD-CANBNK" to "Dear Customer, Acct XXX437 credited with INR 20,000.00 on 03/10/26 from ABHIRAM ARUN; UPI:130616170003; Bal INR 66,972.98-CanaraBank",
    "VM-FEDBNK" to "Dear Customer, Rs.553.33 credited to your A/c XX8868 on 30OCT2026 10:04:55. BAL-Rs.761.19-Federal Bank",
)
