package com.studiomusik.service;

import com.studiomusik.dao.CustomerDAO;
import com.studiomusik.model.Customer;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * CRUD Customer untuk Admin (API_CONTRACTS §4.2). Validasi nama/telepon/email, email unik (BR-20),
 * dan penghapusan Customer yang sudah punya Booking ditolak (BR-22). Customer yang dikembalikan
 * tidak membawa {@code passwordHash}.
 */
public class CustomerService {

    private final CustomerDAO customerDAO;

    public CustomerService() {
        this(new CustomerDAO());
    }

    public CustomerService(CustomerDAO customerDAO) {
        this.customerDAO = customerDAO;
    }

    public List<Customer> getAllCustomers() {
        try {
            List<Customer> result = new ArrayList<>();
            for (Customer customer : customerDAO.findAll()) {
                result.add(ServiceSupport.withoutHash(customer));
            }
            return result;
        } catch (SQLException e) {
            throw ServiceSupport.dbError("membaca daftar customer", e);
        }
    }

    /** @throws ValidationException bila customer tidak ditemukan */
    public Customer getCustomerById(int customerId) {
        try {
            return ServiceSupport.withoutHash(customerDAO.findById(customerId)
                    .orElseThrow(CustomerService::notFound));
        } catch (SQLException e) {
            throw ServiceSupport.dbError("membaca customer", e);
        }
    }

    /**
     * Admin menambah Customer; sama dengan registrasi (validasi, email unik, hash PBKDF2).
     *
     * @throws ValidationException   bila input tidak valid
     * @throws BusinessRuleException bila email sudah terdaftar
     */
    public Customer addCustomer(String name, String phone, String email, String rawPassword) {
        return ServiceSupport.registerNewCustomer(customerDAO, name, phone, email, rawPassword);
    }

    /**
     * Mengubah name, phone, email (password tidak diubah; A-09).
     *
     * @throws ValidationException   bila input tidak valid atau customer tidak ditemukan
     * @throws BusinessRuleException bila email dipakai Customer lain
     */
    public Customer updateCustomer(int customerId, String name, String phone, String email) {
        String cleanName = ServiceSupport.requireText(name, "Nama", ServiceSupport.MAX_PERSON_NAME);
        String cleanPhone = ServiceSupport.normalizePhone(phone);
        String cleanEmail = ServiceSupport.normalizeEmail(email);
        try {
            Customer existing = customerDAO.findById(customerId).orElseThrow(CustomerService::notFound);
            if (customerDAO.existsByEmailExcludingId(cleanEmail, customerId)) {
                throw new BusinessRuleException("Email sudah dipakai customer lain.");
            }
            Customer updated = new Customer(existing.getId(), cleanName, cleanPhone, cleanEmail, null);
            if (!customerDAO.update(updated)) {
                throw notFound();
            }
            return updated;
        } catch (SQLException e) {
            if (ServiceSupport.isDuplicateKey(e)) {
                throw new BusinessRuleException("Email sudah dipakai customer lain.", e);
            }
            throw ServiceSupport.dbError("mengubah customer", e);
        }
    }

    /**
     * Menghapus Customer yang belum punya Booking.
     *
     * @throws BusinessRuleException bila Customer sudah punya Booking (BR-22; FK RESTRICT)
     * @throws ValidationException   bila customer tidak ditemukan
     */
    public void deleteCustomer(int customerId) {
        try {
            if (!customerDAO.delete(customerId)) {
                throw notFound();
            }
        } catch (SQLException e) {
            if (ServiceSupport.isReferencedByChild(e)) {
                throw new BusinessRuleException(
                        "Customer tidak dapat dihapus karena sudah memiliki booking.", e);
            }
            throw ServiceSupport.dbError("menghapus customer", e);
        }
    }

    public long countCustomers() {
        try {
            return customerDAO.count();
        } catch (SQLException e) {
            throw ServiceSupport.dbError("menghitung customer", e);
        }
    }

    private static ValidationException notFound() {
        return new ValidationException("Customer tidak ditemukan.");
    }
}
