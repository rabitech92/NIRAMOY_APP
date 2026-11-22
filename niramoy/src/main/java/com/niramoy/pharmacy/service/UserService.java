package com.niramoy.pharmacy.service;

import com.niramoy.pharmacy.entity.AppUser;
import com.niramoy.pharmacy.entity.Branch;
import com.niramoy.pharmacy.entity.Role;
import com.niramoy.pharmacy.exception.BusinessException;
import com.niramoy.pharmacy.repository.AppUserRepository;
import com.niramoy.pharmacy.repository.BranchRepository;
import com.niramoy.pharmacy.security.UserPrincipal;
import com.niramoy.pharmacy.controller.Forms;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class UserService {
    private final AppUserRepository users;
    private final BranchRepository branches;
    private final PasswordEncoder encoder;
    private final SubscriptionService subscriptions;

    @Transactional(readOnly = true)
    public Page<AppUser> search(Long branchId, String role, String q, int page) {
        Role r = null;
        if (role != null && !role.isBlank()) {
            try { r = Role.valueOf(role); } catch (IllegalArgumentException ignored) { }
        }
        return users.search(branchId, r, q == null ? "" : q.trim(),
                PageRequest.of(Math.max(page, 0), 10, Sort.by("fullName")));
    }

    @Transactional
    public AppUser create(Forms.UserForm f, UserPrincipal actor) {
        subscriptions.checkUserLimit();
        if (users.existsByUsernameIgnoreCase(f.getUsername()))
            throw new BusinessException("এই ইউজারনেম আগেই ব্যবহৃত হয়েছে।");
        // শুধু সুপার অ্যাডমিনই সুপার অ্যাডমিন/মালিক তৈরি করতে পারেন
        if (f.getRole().isGlobal() && actor.getRole() != Role.SUPER_ADMIN)
            throw new BusinessException("এই ভূমিকা তৈরির অনুমতি আপনার নেই।");

        Branch branch = null;
        if (!f.getRole().isGlobal()) {
            if (f.getBranchId() == null) throw new BusinessException("এই ভূমিকার জন্য শাখা নির্বাচন আবশ্যক।");
            branch = branches.findById(f.getBranchId()).orElseThrow(() -> new BusinessException("শাখা পাওয়া যায়নি।"));
        }
        AppUser u = new AppUser();
        u.setUsername(f.getUsername().trim());
        u.setFullName(f.getFullName().trim());
        u.setPassword(encoder.encode(f.getPassword()));
        u.setRole(f.getRole());
        u.setBranch(branch);
        return users.save(u);
    }

    @Transactional
    public void toggleActive(Long id, UserPrincipal actor) {
        AppUser u = users.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (u.getId().equals(actor.getId())) throw new BusinessException("নিজের অ্যাকাউন্ট নিষ্ক্রিয় করা যাবে না।");
        if (u.getRole() == Role.SUPER_ADMIN && actor.getRole() != Role.SUPER_ADMIN)
            throw new BusinessException("সুপার অ্যাডমিনের অবস্থা বদলানোর অনুমতি আপনার নেই।");
        u.setActive(!u.isActive());
    }
}
