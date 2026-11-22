package com.niramoy.pharmacy.config;

import com.niramoy.pharmacy.entity.*;
import com.niramoy.pharmacy.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** ডাটাবেস খালি থাকলে প্রথমবার ডেমো তথ্য ও ডিফল্ট ব্যবহারকারী তৈরি করে। */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "niramoy.seed.demo-data", havingValue = "true")
public class DataSeeder implements ApplicationRunner {

    private final BranchRepository branches;
    private final AppUserRepository users;
    private final MedicineRepository medicines;
    private final SupplierRepository suppliers;
    private final PurchaseOrderRepository orders;
    private final AppSettingRepository settings;
    private final PasswordEncoder encoder;
    private int boxSeq = 0;

    @Value("${niramoy.seed.default-password}")
    private String defaultPassword;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (branches.count() > 0) return;
        log.info("ডেমো তথ্য তৈরি করা হচ্ছে...");

        Branch dhanmondi = branches.save(new Branch("ধানমন্ডি", "ধানমন্ডি, ঢাকা", "০১৭০০-০০০০১১", "তানিয়া রহমান"));
        Branch uttara = branches.save(new Branch("উত্তরা", "উত্তরা, ঢাকা", "০১৭০০-০০০০২২", "আরিফ হোসেন"));
        Branch gulshan = branches.save(new Branch("গুলশান", "গুলশান, ঢাকা", "০১৭০০-০০০০৩৩", "মাহি ইসলাম"));

        user("admin", "সিস্টেম অ্যাডমিন", Role.SUPER_ADMIN, null);
        user("owner", "প্রতিষ্ঠানের মালিক", Role.OWNER, null);
        user("tania", "তানিয়া রহমান", Role.BRANCH_MANAGER, dhanmondi);
        user("arif", "আরিফ হোসেন", Role.PHARMACIST, uttara);
        user("mahi", "মাহি ইসলাম", Role.CASHIER, gulshan);
        user("store", "ইনভেন্টরি ম্যানেজার", Role.INVENTORY_MANAGER, dhanmondi);

        med("নাপা ৫০০ মি.গ্রা.", "ব্যথানাশক", "89010001", "NP-2608", "2027-08-30", "1.40", "2", 284, 60, dhanmondi);
        med("সেকলো ২০ মি.গ্রা.", "গ্যাস্ট্রিক", "89010002", "SC-2703", "2027-03-15", "5.20", "7", 36, 50, dhanmondi);
        med("অ্যামোক্সিল ৫০০ মি.গ্রা.", "অ্যান্টিবায়োটিক", "89010003", "AM-2612", "2026-12-10", "8", "12", 74, 40, uttara);
        med("ওরস্যালাইন", "অন্যান্য", "89010004", "OR-2804", "2028-04-21", "4", "6", 430, 80, gulshan);
        med("ফেক্সো ১২০ মি.গ্রা.", "অ্যালার্জি", "89010005", "FX-2611", "2026-11-28", "7", "10", 18, 35, dhanmondi);
        med("মেটফরমিন ৫০০ মি.গ্রা.", "ডায়াবেটিস", "89010006", "MT-2709", "2027-09-17", "3", "5", 122, 55, uttara);
        med("ক্যালসিয়াম ডি", "সাপ্লিমেন্ট", "89010007", "CD-2706", "2027-06-01", "9", "14", 65, 30, gulshan);
        med("প্যারাসিটামল সিরাপ", "ব্যথানাশক", "89010008", "PS-2612", "2026-12-21", "32", "45", 12, 25, dhanmondi);

        Supplier medi = suppliers.save(new Supplier("মেডি ট্রেডার্স", "০১৭০০-০০০০০১", new BigDecimal("12500")));
        Supplier health = suppliers.save(new Supplier("হেলথ ডিস্ট্রিবিউশন", "০১৭০০-০০০০০২", BigDecimal.ZERO));

        order("PO-1041", health, uttara, "9300", "রফিক (MPO)", OrderStatus.RECEIVED,
                item("Amoxil 500", "Amoxicillin", "Square", 100, "পাতা", Priority.NORMAL, ItemStatus.RECEIVED),
                item("Glucomin 500", "Metformin", "Square", 60, "পাতা", Priority.NORMAL, ItemStatus.RECEIVED));
        order("PO-1042", medi, dhanmondi, "0", "করিম (MPO)", OrderStatus.PENDING,
                item("Napa Extra", "Paracetamol", "Beximco", 200, "পাতা", Priority.URGENT, ItemStatus.ORDERED),
                item("Seclo 20", "Omeprazole", "Square", 100, "পাতা", Priority.NORMAL, ItemStatus.ORDERED),
                item("Fexo 120", "Fexofenadine", "Square", 50, "পাতা", Priority.URGENT, ItemStatus.NOT_ORDERED),
                item("Monas 10", "Montelukast", "Acme", 30, "পাতা", Priority.NORMAL, ItemStatus.NOT_ORDERED));

        settings.save(new AppSetting("vat.percent", "5"));
        settings.save(new AppSetting("invoice.prefix", "INV"));

        log.info("ডেমো ব্যবহারকারী: admin, owner, tania, arif, mahi, store — পাসওয়ার্ড: {}", defaultPassword);
    }

    private void user(String username, String fullName, Role role, Branch branch) {
        AppUser u = new AppUser();
        u.setUsername(username); u.setFullName(fullName); u.setRole(role); u.setBranch(branch);
        u.setPassword(encoder.encode(defaultPassword));
        users.save(u);
    }

    private void med(String name, String category, String barcode, String batch, String expiry,
                     String cost, String price, int stock, int reorder, Branch branch) {
        Medicine m = new Medicine();
        m.setName(name); m.setCategory(category); m.setBarcode(barcode); m.setBatchNo(batch);
        m.setExpiryDate(LocalDate.parse(expiry));
        m.setCostPrice(new BigDecimal(cost)); m.setSalePrice(new BigDecimal(price));
        m.setStock(stock); m.setReorderLevel(reorder); m.setBranch(branch);
        m.setBoxNo("BOX-" + (++boxSeq));
        medicines.save(m);
    }

    private static PurchaseOrderItem item(String brand, String generic, String company, int qty, String unit,
                                          Priority priority, ItemStatus status) {
        PurchaseOrderItem i = new PurchaseOrderItem();
        i.setBrandName(brand); i.setGenericName(generic); i.setCompanyName(company);
        i.setQuantity(qty); i.setUnit(unit); i.setPriority(priority); i.setStatus(status);
        if (status != ItemStatus.NOT_ORDERED) i.setOrderedAt(LocalDateTime.now().minusDays(2));
        if (status == ItemStatus.RECEIVED) i.setReceivedAt(LocalDateTime.now().minusDays(1));
        return i;
    }

    private void order(String no, Supplier supplier, Branch branch, String amount, String mpo, OrderStatus status,
                       PurchaseOrderItem... items) {
        PurchaseOrder o = new PurchaseOrder();
        o.setOrderNo(no); o.setSupplier(supplier); o.setBranch(branch); o.setMpoName(mpo);
        o.setAmount(new BigDecimal(amount)); o.setStatus(status);
        for (PurchaseOrderItem i : items) o.addItem(i);
        o.setItemsSummary(String.join(", ", java.util.Arrays.stream(items).map(PurchaseOrderItem::getBrandName).toList()));
        if (status == OrderStatus.RECEIVED) o.setReceivedAt(LocalDateTime.now().minusDays(1));
        orders.save(o);
    }
}
