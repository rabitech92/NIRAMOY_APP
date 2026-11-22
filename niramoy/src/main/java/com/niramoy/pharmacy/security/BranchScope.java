package com.niramoy.pharmacy.security;

import com.niramoy.pharmacy.entity.Branch;
import com.niramoy.pharmacy.exception.BusinessException;
import com.niramoy.pharmacy.repository.BranchRepository;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * কোন শাখার তথ্য দেখানো হবে তা ঠিক করে।
 * - শাখা-সীমিত ব্যবহারকারী (ম্যানেজার, ফার্মাসিস্ট ইত্যাদি) সবসময় নিজের শাখায় আটকে থাকেন।
 * - SUPER_ADMIN / OWNER ওপরের ড্রপডাউন থেকে শাখা বা "সব শাখা" বেছে নিতে পারেন।
 */
@Component
@RequiredArgsConstructor
public class BranchScope {
    private static final String SESSION_KEY = "ACTIVE_BRANCH_ID";
    private final BranchRepository branches;

    /** null মানে "সব শাখা" */
    public Long current() {
        UserPrincipal user = UserPrincipal.current();
        if (user == null) return null;
        if (!user.isGlobal()) return user.getBranchId();
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            // মোবাইল অ্যাপ (stateless): X-Branch-Id হেডার
            String h = attrs.getRequest().getHeader("X-Branch-Id");
            if (h != null && !h.isBlank()) {
                try {
                    long id = Long.parseLong(h.trim());
                    if (branches.existsById(id)) return id;
                } catch (NumberFormatException ignored) { }
            }
            HttpSession session = attrs.getRequest().getSession(false);
            if (session != null && session.getAttribute(SESSION_KEY) instanceof Long id) return id;
        }
        return null;
    }

    /** লেখার কাজে (নতুন ওষুধ, বিক্রয় ইত্যাদি) নির্দিষ্ট একটি শাখা আবশ্যক */
    public Branch require() {
        Long id = current();
        if (id == null) throw new BusinessException("এই কাজের জন্য ওপরের ড্রপডাউন থেকে একটি শাখা নির্বাচন করুন।");
        return branches.findById(id).orElseThrow(() -> new BusinessException("শাখা পাওয়া যায়নি।"));
    }

    public void switchTo(Long branchId, HttpSession session) {
        UserPrincipal user = UserPrincipal.current();
        if (user == null || !user.isGlobal()) return;   // সীমিত ব্যবহারকারী শাখা বদলাতে পারবেন না
        if (branchId == null) session.removeAttribute(SESSION_KEY);
        else if (branches.existsById(branchId)) session.setAttribute(SESSION_KEY, branchId);
    }
}
