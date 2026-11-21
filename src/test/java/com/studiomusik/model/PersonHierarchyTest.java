package com.studiomusik.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;

import org.junit.jupiter.api.Test;

/** AC-M01 (Person abstract) dan AC-M02 (getRole polimorfik). */
class PersonHierarchyTest {

    @Test
    void personIsAbstractAndDeclaresAbstractGetRole() throws Exception {
        assertTrue(Modifier.isAbstract(Person.class.getModifiers()));
        Method getRole = Person.class.getDeclaredMethod("getRole");
        assertTrue(Modifier.isAbstract(getRole.getModifiers()));
    }

    @Test
    void personCannotBeInstantiatedEvenViaReflection() throws Exception {
        Constructor<Person> ctor = Person.class.getDeclaredConstructor(int.class, String.class, String.class, String.class);
        ctor.setAccessible(true);
        assertThrows(java.lang.InstantiationException.class, () -> ctor.newInstance(1, "a", "b", "c"));
    }

    @Test
    void customerAndAdminOverrideGetRoleThemselves() throws Exception {
        Method customerRole = Customer.class.getDeclaredMethod("getRole");
        Method adminRole = Admin.class.getDeclaredMethod("getRole");
        assertEquals(Customer.class, customerRole.getDeclaringClass());
        assertEquals(Admin.class, adminRole.getDeclaringClass());
        assertFalse(Modifier.isAbstract(customerRole.getModifiers()));
        assertFalse(Modifier.isAbstract(adminRole.getModifiers()));
    }

    @Test
    void getRoleIsPolymorphicThroughPersonReference() {
        Person customer = Fixtures.customer();
        Person admin = new Admin(1, "Administrator Studio", "admin", "hash-uji");
        assertEquals("Customer", customer.getRole());
        assertEquals("Admin", admin.getRole());

        List<Person> users = List.of(customer, admin);
        assertEquals(List.of("Customer", "Admin"), users.stream().map(Person::getRole).toList());
    }

    @Test
    void customerAndAdminInheritFromPerson() {
        assertEquals(Person.class, Customer.class.getSuperclass());
        assertEquals(Person.class, Admin.class.getSuperclass());

        Customer customer = Fixtures.customer();
        assertEquals(1, customer.getId());
        assertEquals("Made Ayu Lestari", customer.getName());
        assertEquals("081234567801", customer.getPhone());
        assertEquals("ayu.lestari@example.com", customer.getEmail());
        assertEquals("hash-uji", customer.getPasswordHash());
    }

    @Test
    void adminHasNoPhoneOrEmailBecauseTableHasNoSuchColumns() {
        Admin admin = new Admin(1, "Administrator Studio", "admin", "hash-uji");
        assertNull(admin.getPhone());
        assertNull(admin.getEmail());
        assertEquals("admin", admin.getUsername());
        assertEquals("hash-uji", admin.getPasswordHash());
    }

    @Test
    void settersOfPersonUpdateInheritedState() {
        Customer customer = Fixtures.customer();
        customer.setId(9);
        customer.setName("Nama Baru");
        customer.setPhone("0811");
        customer.setEmail("baru@example.com");
        assertEquals(9, customer.getId());
        assertEquals("Nama Baru", customer.getName());
        assertEquals("0811", customer.getPhone());
        assertEquals("baru@example.com", customer.getEmail());
    }

    @Test
    void roleIsNotAffectedByPersonDataAndInstancesAreIndependent() {
        Customer a = Fixtures.customer();
        Customer b = new Customer(2, "Lain", "0812", "lain@example.com", "hash-lain");
        assertEquals(a.getRole(), b.getRole());
        assertFalse(a == b);
        assertSame(a, a);
    }
}
