package dev.expensetracker.data

/** Topic → subtopics. `icon` values are keys into ui/CategoryIcons.kt. Seeded idempotently by name on every launch. */
data class TopicDef(val name: String, val color: Long, val icon: String, val subs: List<Pair<String, String>>)

val TAXONOMY = listOf(
    TopicDef("Food & Dining", 0xFFF4511E, "Restaurant", listOf(
        "Restaurants" to "Restaurant", "Cafes & Coffee" to "LocalCafe", "Food Delivery" to "DeliveryDining",
        "Fast Food" to "Fastfood", "Bars & Pubs" to "LocalBar", "Bakery & Desserts" to "BakeryDining",
        "Street Food" to "RamenDining", "Office Canteen" to "LunchDining", "Ice Cream & Juices" to "Icecream",
    )),
    TopicDef("Groceries", 0xFF43A047, "ShoppingCart", listOf(
        "Supermarket" to "LocalGroceryStore", "Fruits & Vegetables" to "Eco", "Meat & Fish" to "SetMeal",
        "Dairy & Eggs" to "LocalDrink", "Online Grocery" to "LocalShipping", "Kirana Store" to "Storefront",
        "Household Supplies" to "CleaningServices",
    )),
    TopicDef("Transport", 0xFF1E88E5, "DirectionsCar", listOf(
        "Fuel" to "LocalGasStation", "Cab & Auto" to "LocalTaxi", "Metro" to "Subway", "Bus" to "DirectionsBus",
        "Parking" to "LocalParking", "Tolls & FASTag" to "Toll", "Vehicle Service" to "CarRepair",
        "Bike & Scooter Rental" to "TwoWheeler", "EV Charging" to "EvStation", "Vehicle Insurance" to "Shield",
    )),
    TopicDef("Travel", 0xFF00ACC1, "Flight", listOf(
        "Flights" to "Flight", "Hotels & Stays" to "Hotel", "Train Tickets" to "Train", "Bus Tickets" to "DirectionsBus",
        "Holiday Packages" to "BeachAccess", "Visa & Passport" to "Public", "Forex" to "CurrencyExchange",
        "Travel Insurance" to "HealthAndSafety", "Sightseeing & Tours" to "Attractions",
    )),
    TopicDef("Shopping", 0xFFD81B60, "ShoppingBag", listOf(
        "Clothing" to "Checkroom", "Footwear" to "ShoppingBag", "Electronics" to "Devices",
        "Mobile & Accessories" to "PhoneAndroid", "Online Marketplace" to "LocalMall", "Home & Kitchen" to "Kitchen",
        "Furniture" to "Chair", "Jewellery & Watches" to "Watch", "Beauty & Cosmetics" to "Face",
        "Books & Stationery" to "MenuBook", "Toys & Games" to "Toys",
    )),
    TopicDef("Housing", 0xFF8D6E63, "Home", listOf(
        "Rent" to "Home", "Maintenance & Society" to "Apartment", "Repairs & Plumbing" to "Handyman",
        "Furnishing & Decor" to "Weekend", "Domestic Help" to "CleaningServices", "Property Tax" to "AccountBalance",
        "Security Deposit" to "Lock",
    )),
    TopicDef("Bills & Utilities", 0xFFFFA000, "Receipt", listOf(
        "Electricity" to "Bolt", "Water" to "WaterDrop", "Gas & LPG" to "LocalFireDepartment",
        "Mobile Recharge" to "PhoneAndroid", "Postpaid Bill" to "Phone", "Broadband & Wi-Fi" to "Wifi",
        "DTH & Cable" to "Tv",
    )),
    TopicDef("Subscriptions", 0xFF5E35B1, "Subscriptions", listOf(
        "Video Streaming" to "LiveTv", "Music Streaming" to "MusicNote", "Cloud Storage" to "Cloud",
        "Software & Apps" to "Apps", "AI Tools" to "AutoAwesome", "News & Magazines" to "Newspaper",
        "Gaming" to "SportsEsports", "Memberships" to "CardMembership",
    )),
    TopicDef("Health", 0xFFE53935, "LocalHospital", listOf(
        "Pharmacy" to "LocalPharmacy", "Doctor Consultation" to "MedicalServices", "Hospital" to "LocalHospital",
        "Lab Tests" to "Biotech", "Dental" to "MedicalServices", "Eye Care" to "Visibility",
        "Health Insurance" to "HealthAndSafety", "Therapy & Counselling" to "Psychology",
    )),
    TopicDef("Fitness & Wellness", 0xFF00897B, "FitnessCenter", listOf(
        "Gym" to "FitnessCenter", "Yoga & Classes" to "SelfImprovement", "Sports" to "SportsTennis",
        "Salon & Grooming" to "ContentCut", "Spa & Massage" to "Spa", "Supplements" to "Medication",
    )),
    TopicDef("Entertainment", 0xFF8E24AA, "TheaterComedy", listOf(
        "Movies" to "Movie", "Concerts & Events" to "Celebration", "Amusement & Outings" to "Attractions",
        "Hobbies" to "Palette", "Gaming" to "SportsEsports", "Photography" to "PhotoCamera",
    )),
    TopicDef("Education", 0xFF3949AB, "School", listOf(
        "School & College Fees" to "School", "Courses & Coaching" to "CastForEducation",
        "Books & Supplies" to "MenuBook", "Exam Fees" to "Quiz", "Online Learning" to "Laptop",
    )),
    TopicDef("Family & Personal", 0xFFEC407A, "FamilyRestroom", listOf(
        "Kids & Childcare" to "ChildCare", "Pets" to "Pets", "Elder Care" to "Elderly", "Personal Care" to "Face",
        "Laundry & Dry Cleaning" to "LocalLaundryService", "Pocket Money" to "Payments",
    )),
    TopicDef("Finance", 0xFF546E7A, "AccountBalance", listOf(
        "Loan EMI" to "Payments", "Credit Card Bill" to "CreditCard", "Insurance Premium" to "Shield",
        "Investments & SIP" to "TrendingUp", "Stocks" to "ShowChart", "Gold" to "Diamond",
        "Bank Charges & Fees" to "AccountBalance", "Taxes" to "Gavel", "Interest Paid" to "Percent",
    )),
    TopicDef("Transfers", 0xFF78909C, "SwapHoriz", listOf(
        "To Family" to "FamilyRestroom", "To Friends" to "People", "Self Transfer" to "SyncAlt",
        "Bill Split" to "CallSplit", "Wallet Top-up" to "AccountBalanceWallet", "Cash Withdrawal" to "LocalAtm",
    )),
    TopicDef("Giving", 0xFF7CB342, "VolunteerActivism", listOf(
        "Charity" to "VolunteerActivism", "Temple & Religious" to "TempleHindu", "Gifts to Others" to "CardGiftcard",
        "Crowdfunding" to "Groups",
    )),
    TopicDef("Income", 0xFF2E7D32, "Payments", listOf(
        "Salary" to "Work", "Freelance & Side Gigs" to "Laptop", "Business Income" to "Storefront",
        "Refunds" to "Replay", "Cashback & Rewards" to "Loyalty", "Interest Earned" to "Savings",
        "Dividends" to "ShowChart", "Rent Received" to "Home", "Reimbursement" to "ReceiptLong",
        "Gifts Received" to "Redeem", "From Family" to "FamilyRestroom",
    )),
    TopicDef("Other", 0xFF9E9E9E, "MoreHoriz", listOf(
        "Miscellaneous" to "MoreHoriz", "Fees & Fines" to "Gavel", "Unknown" to "HelpOutline",
    )),
)

val TOPIC_ORDER: Map<String, Int> = TAXONOMY.mapIndexed { i, t -> t.name to i }.toMap()
