package com.niramoy.pharmacy.controller;

import com.niramoy.pharmacy.entity.*;
import com.niramoy.pharmacy.exception.BusinessException;
import com.niramoy.pharmacy.security.BranchScope;
import com.niramoy.pharmacy.service.PurchaseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/purchases")
@RequiredArgsConstructor
public class PurchaseController {
    private final PurchaseService service;
    private final BranchScope scope;

    @GetMapping
    public String list(@RequestParam(defaultValue = "orders") String tab, @RequestParam(defaultValue = "") String q,
                       @RequestParam(defaultValue = "") String filter, @RequestParam(defaultValue = "0") int page,
                       Model model) {
        switch (tab) {
            case "suppliers" -> model.addAttribute("page", service.suppliers(q, filter, page));
            case "returns" -> model.addAttribute("page", service.returns(scope.current(), q, page));
            default -> { tab = "orders"; model.addAttribute("page", service.orders(scope.current(), q, filter, page)); }
        }
        model.addAttribute("tab", tab);
        model.addAttribute("q", q);
        model.addAttribute("filter", filter);
        model.addAttribute("orderStatuses", OrderStatus.values());
        return "purchases/list";
    }

    // ---------- suppliers ----------
    @GetMapping("/suppliers/new")
    public String supplierForm(Model model) {
        model.addAttribute("form", new Forms.SupplierForm());
        return "purchases/supplier-form";
    }

    @PostMapping("/suppliers")
    public String createSupplier(@Valid @ModelAttribute("form") Forms.SupplierForm form, BindingResult result,
                                 RedirectAttributes ra) {
        if (result.hasErrors()) return "purchases/supplier-form";
        service.createSupplier(form);
        ra.addFlashAttribute("success", "সরবরাহকারী যোগ হয়েছে।");
        return "redirect:/purchases?tab=suppliers";
    }

    // ---------- order (ক্রয় তালিকা) ----------
    @GetMapping("/orders/new")
    public String orderForm(Model model) {
        Forms.OrderForm form = new Forms.OrderForm();
        for (int i = 0; i < 3; i++) form.getItems().add(new Forms.ItemRow());
        model.addAttribute("form", form);
        orderFormModel(model);
        return "purchases/order-form";
    }

    @PostMapping("/orders")
    public String createOrder(@Valid @ModelAttribute("form") Forms.OrderForm form, BindingResult result,
                              RedirectAttributes ra, Model model) {
        orderFormModel(model);
        if (form.getItems().isEmpty()) form.getItems().add(new Forms.ItemRow());
        if (result.hasErrors()) return "purchases/order-form";
        try {
            PurchaseOrder o = service.createOrder(form, scope.require());
            ra.addFlashAttribute("success", "ক্রয় তালিকা তৈরি হয়েছে: " + o.getOrderNo()
                    + "। এখন এমপিওকে দেখান এবং তিনি যেগুলোর অর্ডার নেন সেগুলো 'অর্ডার সম্পন্ন' করুন।");
            return "redirect:/purchases/orders/" + o.getId();
        } catch (BusinessException e) {
            model.addAttribute("error", e.getMessage());
            return "purchases/order-form";
        }
    }

    @GetMapping("/orders/{id}")
    public String orderDetail(@PathVariable Long id, Model model) {
        model.addAttribute("o", service.getOrder(id));
        if (!model.containsAttribute("returnForm")) model.addAttribute("returnForm", new Forms.PurchaseReturnForm());
        if (!model.containsAttribute("itemForm")) model.addAttribute("itemForm", new Forms.ItemRow());
        model.addAttribute("priorities", Priority.values());
        model.addAttribute("units", PurchaseService.UNITS);
        return "purchases/order-detail";
    }

    @PostMapping("/orders/{id}/items")
    public String addItem(@PathVariable Long id, @ModelAttribute("itemForm") Forms.ItemRow row, RedirectAttributes ra) {
        try {
            service.addItem(id, row);
            ra.addFlashAttribute("success", "আইটেম যোগ হয়েছে।");
        } catch (BusinessException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/purchases/orders/" + id;
    }

    @PostMapping("/orders/{id}/mark-ordered")
    public String markOrdered(@PathVariable Long id, @RequestParam(name = "itemIds", required = false) List<Long> itemIds,
                              RedirectAttributes ra) {
        try {
            int n = service.markOrdered(id, itemIds);
            ra.addFlashAttribute("success", n + "টি আইটেম 'অর্ডার সম্পন্ন' চিহ্নিত হয়েছে। বাকিগুলো 'অর্ডার হয়নি' থাকবে।");
        } catch (BusinessException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/purchases/orders/" + id;
    }

    @PostMapping("/orders/{id}/items/{itemId}/unorder")
    public String unorder(@PathVariable Long id, @PathVariable Long itemId, RedirectAttributes ra) {
        try {
            service.unmarkOrdered(id, itemId);
            ra.addFlashAttribute("success", "অর্ডার বাতিল করা হয়েছে; আইটেমটি 'অর্ডার হয়নি' অবস্থায় ফিরেছে।");
        } catch (BusinessException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/purchases/orders/" + id;
    }

    @PostMapping("/orders/{id}/items/{itemId}/delete")
    public String deleteItem(@PathVariable Long id, @PathVariable Long itemId, RedirectAttributes ra) {
        try {
            service.removeItem(id, itemId);
            ra.addFlashAttribute("success", "আইটেম মুছে ফেলা হয়েছে।");
        } catch (BusinessException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/purchases/orders/" + id;
    }

    // ---------- পণ্য গ্রহণ (ফার্মাসিস্ট স্টকে যোগ করেন) ----------
    @GetMapping("/orders/{id}/items/{itemId}/receive")
    public String receiveForm(@PathVariable Long id, @PathVariable Long itemId, Model model, RedirectAttributes ra) {
        PurchaseOrder o = service.getOrder(id);
        PurchaseOrderItem item = service.getItem(o, itemId);
        if (item.getStatus() != ItemStatus.ORDERED) {
            ra.addFlashAttribute("error", "শুধু 'অর্ডার সম্পন্ন' আইটেমের পণ্য গ্রহণ করা যায়।");
            return "redirect:/purchases/orders/" + id;
        }
        model.addAttribute("o", o);
        model.addAttribute("item", item);
        model.addAttribute("receiveForm", prefill(item));
        return "purchases/item-receive";
    }

    @PostMapping("/orders/{id}/items/{itemId}/receive")
    public String receive(@PathVariable Long id, @PathVariable Long itemId,
                          @Valid @ModelAttribute("receiveForm") Forms.ReceiveForm form, BindingResult result,
                          RedirectAttributes ra, Model model) {
        PurchaseOrder o = service.getOrder(id);
        PurchaseOrderItem item = service.getItem(o, itemId);
        model.addAttribute("o", o);
        model.addAttribute("item", item);
        if (result.hasErrors()) return "purchases/item-receive";
        try {
            service.receiveItem(id, itemId, form);
            ra.addFlashAttribute("success", "'" + item.getBrandName() + "' স্টকে যোগ হয়েছে।");
            return "redirect:/purchases/orders/" + id;
        } catch (BusinessException e) {
            model.addAttribute("error", e.getMessage());
            return "purchases/item-receive";
        }
    }

    @PostMapping("/orders/{id}/return")
    public String purchaseReturn(@PathVariable Long id, @Valid @ModelAttribute("returnForm") Forms.PurchaseReturnForm form,
                                 BindingResult result, RedirectAttributes ra, Model model) {
        if (result.hasErrors()) {
            model.addAttribute("o", service.getOrder(id));
            model.addAttribute("itemForm", new Forms.ItemRow());
            model.addAttribute("priorities", Priority.values());
            model.addAttribute("units", PurchaseService.UNITS);
            return "purchases/order-detail";
        }
        try {
            service.returnToSupplier(id, form);
            ra.addFlashAttribute("success", "ক্রয় ফেরত নথিভুক্ত হয়েছে।");
        } catch (BusinessException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/purchases/orders/" + id;
    }

    private void orderFormModel(Model model) {
        model.addAttribute("suppliers", service.allSuppliers());
        model.addAttribute("priorities", Priority.values());
        model.addAttribute("units", PurchaseService.UNITS);
    }

    private Forms.ReceiveForm prefill(PurchaseOrderItem item) {
        Forms.ReceiveForm f = new Forms.ReceiveForm();
        f.setName(item.getBrandName());
        f.setGenericName(item.getGenericName());
        f.setCompanyName(item.getCompanyName());
        if ("পাতা".equals(item.getUnit())) f.setQuantityStrips(item.getQuantity());
        else f.setQuantityPieces(item.getQuantity());
        return f;
    }
}
