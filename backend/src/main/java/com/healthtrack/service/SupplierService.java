package com.healthtrack.service;

import com.healthtrack.dto.PharmacyDtos.*;
import com.healthtrack.entity.*;
import com.healthtrack.event.EventConstants;
import com.healthtrack.event.EventPublisher;
import com.healthtrack.repository.*;
import com.healthtrack.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SupplierService {

    private final SupplierRepository supplierRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final MedicineStockRepository medicineStockRepository;
    private final HospitalRepository hospitalRepository;
    private final EventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public List<SupplierResponse> getSuppliers(UserPrincipal currentUser) {
        return supplierRepository.findByHospitalId(currentUser.getHospitalId())
                .stream().map(this::mapSupplier).toList();
    }

    @Transactional
    public SupplierResponse createSupplier(CreateSupplierRequest request, UserPrincipal currentUser) {
        Hospital hospital = hospitalRepository.getReferenceById(currentUser.getHospitalId());
        Supplier supplier = Supplier.builder()
                .hospital(hospital)
                .name(request.name())
                .contactPerson(request.contactPerson())
                .contactNumber(request.contactNumber())
                .email(request.email())
                .address(request.address())
                .gstNumber(request.gstNumber())
                .build();
        supplier = supplierRepository.save(supplier);
        return mapSupplier(supplier);
    }

    @Transactional
    public SupplierResponse updateSupplier(Long id, CreateSupplierRequest request, UserPrincipal currentUser) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Supplier not found"));
        supplier.setName(request.name());
        supplier.setContactPerson(request.contactPerson());
        supplier.setContactNumber(request.contactNumber());
        supplier.setEmail(request.email());
        supplier.setAddress(request.address());
        supplier.setGstNumber(request.gstNumber());
        supplier = supplierRepository.save(supplier);
        return mapSupplier(supplier);
    }

    @Transactional
    public void toggleSupplierStatus(Long id, UserPrincipal currentUser) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Supplier not found"));
        supplier.setIsActive(!supplier.getIsActive());
        supplierRepository.save(supplier);
    }

    @Transactional(readOnly = true)
    public List<PurchaseOrderResponse> getPurchaseOrders(UserPrincipal currentUser) {
        return purchaseOrderRepository.findByHospitalIdOrderByOrderedAtDesc(currentUser.getHospitalId())
                .stream().map(this::mapPurchaseOrder).toList();
    }

    @Transactional
    public PurchaseOrderResponse createPurchaseOrder(CreatePurchaseOrderRequest request, UserPrincipal currentUser) {
        Hospital hospital = hospitalRepository.getReferenceById(currentUser.getHospitalId());
        Supplier supplier = null;
        if (request.supplierId() != null) {
            supplier = supplierRepository.findById(request.supplierId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Supplier not found"));
        }

        BigDecimal totalPrice = request.totalPrice();
        if (totalPrice == null && request.unitPrice() != null) {
            totalPrice = request.unitPrice().multiply(BigDecimal.valueOf(request.quantityOrdered()));
        }

        PurchaseOrder order = PurchaseOrder.builder()
                .hospital(hospital)
                .supplier(supplier)
                .medicineName(request.medicineName())
                .quantityOrdered(request.quantityOrdered())
                .unitPrice(request.unitPrice())
                .totalPrice(totalPrice)
                .notes(request.notes())
                .status(PurchaseOrderStatus.CREATED)
                .build();
        order = purchaseOrderRepository.save(order);

        eventPublisher.publish(EventConstants.REORDER_CREATED, currentUser.getHospitalId(),
                Map.of("purchaseOrderId", order.getId(), "medicineName", order.getMedicineName(),
                        "quantity", order.getQuantityOrdered(),
                        "supplierName", supplier != null ? supplier.getName() : "N/A"));
        return mapPurchaseOrder(order);
    }

    @Transactional
    public PurchaseOrderResponse receivePurchaseOrder(Long orderId, UserPrincipal currentUser) {
        PurchaseOrder order = purchaseOrderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Purchase order not found"));
        order.setStatus(PurchaseOrderStatus.RECEIVED);
        order.setQuantityReceived(order.getQuantityOrdered());
        order.setReceivedAt(OffsetDateTime.now());
        order = purchaseOrderRepository.save(order);

        return mapPurchaseOrder(order);
    }

    @Transactional
    public PurchaseOrderResponse cancelPurchaseOrder(Long orderId, UserPrincipal currentUser) {
        PurchaseOrder order = purchaseOrderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Purchase order not found"));
        order.setStatus(PurchaseOrderStatus.CANCELLED);
        order = purchaseOrderRepository.save(order);
        return mapPurchaseOrder(order);
    }

    @Transactional(readOnly = true)
    public List<ReorderSuggestionResponse> getReorderSuggestions(UserPrincipal currentUser) {
        Long hospitalId = currentUser.getHospitalId();
        List<MedicineStock> lowStock = medicineStockRepository.findLowStockByHospitalId(hospitalId);
        return lowStock.stream()
                .filter(s -> !s.isExpired())
                .map(s -> new ReorderSuggestionResponse(
                        s.getMedicineName(),
                        s.getEffectiveQuantity(),
                        s.getReorderLevel(),
                        Math.max(s.getReorderLevel() * 2 - s.getEffectiveQuantity(), s.getReorderLevel()),
                        "Stock below reorder level (" + s.getReorderLevel() + ")"))
                .toList();
    }

    private SupplierResponse mapSupplier(Supplier s) {
        return new SupplierResponse(
                s.getId(), s.getName(), s.getContactPerson(),
                s.getContactNumber(), s.getEmail(), s.getAddress(),
                s.getGstNumber(), s.getIsActive());
    }

    private PurchaseOrderResponse mapPurchaseOrder(PurchaseOrder po) {
        return new PurchaseOrderResponse(
                po.getId(),
                po.getSupplier() != null ? po.getSupplier().getId() : null,
                po.getSupplier() != null ? po.getSupplier().getName() : null,
                po.getMedicineName(),
                po.getQuantityOrdered(),
                po.getQuantityReceived(),
                po.getUnitPrice(),
                po.getTotalPrice(),
                po.getStatus().name(),
                po.getNotes(),
                po.getOrderedAt() != null ? po.getOrderedAt().toString() : null,
                po.getReceivedAt() != null ? po.getReceivedAt().toString() : null);
    }
}
