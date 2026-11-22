package com.niramoy.pharmacy.service;

import com.niramoy.pharmacy.entity.Branch;
import com.niramoy.pharmacy.exception.BusinessException;
import com.niramoy.pharmacy.repository.BranchRepository;
import com.niramoy.pharmacy.controller.Forms;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BranchService {
    private final BranchRepository branches;
    private final SubscriptionService subscriptions;

    @Transactional(readOnly = true)
    public List<Branch> all() { return branches.findAllByOrderByNameAsc(); }

    @Transactional
    public Branch create(Forms.BranchForm f) {
        subscriptions.checkBranchLimit();
        if (branches.existsByNameIgnoreCase(f.getName().trim())) throw new BusinessException("এই নামে শাখা আগেই আছে।");
        return branches.save(new Branch(f.getName().trim(), f.getAddress(), f.getPhone(), f.getManagerName()));
    }

    @Transactional
    public void toggle(Long id) {
        Branch b = branches.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (b.isActive() && branches.countByActiveTrue() <= 1) throw new BusinessException("কমপক্ষে একটি শাখা সক্রিয় থাকতে হবে।");
        b.setActive(!b.isActive());
    }
}
