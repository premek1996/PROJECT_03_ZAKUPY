package com.app.service;


import com.app.persistence.converter.CustomerWithProductsConverter;
import com.app.persistence.model.Category;
import com.app.persistence.model.Customer;
import com.app.persistence.model.CustomerWithProducts;
import com.app.persistence.model.Product;
import com.app.persistence.validator.CustomerWithProductsValidator;
import com.app.service.exception.OrdersServiceException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.*;

public class OrdersService {

    private final Map<Customer, Map<Product, Long>> customersWithProducts;

    public OrdersService(List<String> jsonFilenames) {
        this.customersWithProducts = init(jsonFilenames);
    }

    private Map<Customer, Map<Product, Long>> init(List<String> jsonFilenames) {
        CustomerWithProductsValidator customerWithProductsValidator = new CustomerWithProductsValidator();
        List<CustomerWithProducts> customerWithProducts = jsonFilenames
                .stream()
                .flatMap(filename -> new CustomerWithProductsConverter(filename)
                        .fromJson()
                        .orElseThrow(() -> new OrdersServiceException("Cannot open json file %s".formatted(filename)))
                        .stream())
                .peek(customerWithProductsValidator::validate)
                .toList();

        return customerWithProducts
                .stream()
                .collect(groupingBy(
                        CustomerWithProducts::getCustomer,
                        Collectors.collectingAndThen(
                                Collectors.flatMapping(cwp -> cwp.getProducts().stream(), toList()),
                                products -> products
                                        .stream()
                                        .collect(groupingBy(Function.identity(), counting()))
                        )
                ));
    }

    /*
        Wyznacz klienta, który zapłacił najwięcej za wszystkie zakupy.
    */
    public Customer getCustomerWithMaxExpense() {
        return customersWithProducts.entrySet()
                .stream()
                .max(Comparator.comparing(entry -> getExpense(entry.getValue())))
                .map(Map.Entry::getKey)
                .orElseThrow();
    }

    private static BigDecimal getExpense(Map<Product, Long> products) {
        return products.entrySet()
                .stream()
                .map(OrdersService::getTotalPrice)
                .reduce(BigDecimal::add)
                .orElse(BigDecimal.ZERO);
    }

    private static BigDecimal getTotalPrice(Map.Entry<Product, Long> entry) {
        return entry.getKey().getPrice().multiply(BigDecimal.valueOf(entry.getValue()));
    }

    /*
      W osobnej metodzie wyznacz klienta, który zapłacił najwięcej
      za zakupy z wybranej kategorii. Nazwę kategorii przekaż jako
      argument funkcji.
  */
    public Customer getCustomerWithMaxExpenseOnCategory(Category category) {
        return customersWithProducts.entrySet()
                .stream()
                .max(Comparator.comparing(entry -> getExpense(entry.getValue(), category)))
                .map(Map.Entry::getKey)
                .orElseThrow();
    }

    private static BigDecimal getExpense(Map<Product, Long> products, Category category) {
        return products.entrySet()
                .stream()
                .filter(entry -> entry.getKey().hasCategory(category))
                .map(OrdersService::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /*
       Wykonaj zestawienie (mapę), w którym pokażesz wiek klientów oraz
       kategorie produktów, które najchętniej w tym wieku kupowano.
    */
    public Map<Integer, Category> getAgesWithPopularCategories() {
        return customersWithProducts.entrySet()
                .stream()
                .collect(Collectors.groupingBy(entry -> entry.getKey().getAge()))
                .entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> getPopularCategory(entry.getValue())));
    }

    private static Category getPopularCategory(List<Map.Entry<Customer, Map<Product, Long>>> customersWithProducts) {
        return customersWithProducts
                .stream()
                .map(Map.Entry::getValue)
                .flatMap(m -> m.entrySet().stream())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, Long::sum))
                .entrySet()
                .stream()
                .collect(Collectors.groupingBy(entry -> entry.getKey().getCategory()))
                .entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> getQuantity(entry.getValue())))
                .entrySet()
                .stream()
                .max(Comparator.comparingLong(Map.Entry::getValue))
                .map(Map.Entry::getKey)
                .orElseThrow();
    }

    private static Long getQuantity(List<Map.Entry<Product, Long>> products) {
        return products.stream()
                .map(Map.Entry::getValue)
                .reduce(0L, Long::sum);
    }

    /*
        Wykonaj zestawienie (mapę), w którym pokażesz średnią cenę produktów
        w danej kategorii.
     */
    public Map<Category, BigDecimal> getCategoriesWithAveragePrices() {
        return customersWithProducts.values()
                .stream()
                .map(Map::keySet)
                .flatMap(Set::stream)
                .collect(Collectors.groupingBy(Product::getCategory))
                .entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> getAveragePrice(entry.getValue())));
    }

    private static BigDecimal getAveragePrice(List<Product> products) {
        BigDecimal sum = products.stream()
                .map(Product::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(products.size()), RoundingMode.CEILING);
    }

    /*
        Dodatkowo wyznacz dla każdej kategorii produkt
        najdroższy oraz produkt najtańszy.
    */
    public Map<Category, Product> getCategoriesAndProductsWithMaxPrice() {
        return customersWithProducts.values()
                .stream()
                .map(Map::keySet)
                .flatMap(Set::stream)
                .collect(Collectors.groupingBy(Product::getCategory))
                .entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> getProductWithMaxPrice(entry.getValue())));
    }

    private static Product getProductWithMaxPrice(List<Product> products) {
        return products.stream()
                .max(Comparator.comparing(Product::getPrice))
                .orElseThrow();
    }

    public Map<Category, Product> getCategoriesAndProductsWithMinPrice() {
        return customersWithProducts.values()
                .stream()
                .map(Map::keySet)
                .flatMap(Set::stream)
                .collect(Collectors.groupingBy(Product::getCategory))
                .entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> getProductWithMinPrice(entry.getValue())));
    }

    private static Product getProductWithMinPrice(List<Product> products) {
        return products.stream()
                .min(Comparator.comparing(Product::getPrice))
                .orElseThrow();
    }

    /*
        Wyznacz klientów, którzy kupowali najczęściej produkty danej
        kategorii. Otrzymane zestawienie zwracaj w postaci mapy.
     */
    public Map<Category, Customer> getCategoriesAndCustomers() {
        Map<Customer, Map<Category, Long>> customersWithCategoriesQuantity = getCustomersWithCategoriesQuantity();
        return Arrays.stream(Category.values())
                .collect(Collectors.toMap(Function.identity(), category -> getCustomerWithMaxProductsQuantityOfGivenCategory(category, customersWithCategoriesQuantity)));
    }

    private Map<Customer, Map<Category, Long>> getCustomersWithCategoriesQuantity() {
        return customersWithProducts
                .entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> getCategoriesAndProductsNumbers(entry.getValue())));
    }

    private static Customer getCustomerWithMaxProductsQuantityOfGivenCategory(Category category,
                                                                              Map<Customer, Map<Category, Long>> customersWithCategoriesQuantity) {
        return customersWithCategoriesQuantity.entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> getQuantityOfGivenCategory(category, entry.getValue())))
                .entrySet()
                .stream()
                .max(Comparator.comparingLong(Map.Entry::getValue))
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    private static Long getQuantityOfGivenCategory(Category category, Map<Category, Long> categoriesWithQuantities) {
        return categoriesWithQuantities.entrySet()
                .stream()
                .filter(entry -> entry.getKey() == category)
                .findAny()
                .map(Map.Entry::getValue)
                .orElse(0L);
    }

    private static Map<Category, Long> getCategoriesAndProductsNumbers(Map<Product, Long> products) {
        return products.entrySet()
                .stream()
                .collect(Collectors.groupingBy(entry -> entry.getKey().getCategory()))
                .entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> getQuantity(entry.getValue())));
    }

    /*
        Sprawdź, czy klient jest w stanie zapłacić za zakupy. Żeby
        to stwierdzić, porównaj wartość pola przechowującego ilość gotówki,
        którą posiada klient z sumaryczną ceną za zakupy klienta. Wykonaj
        mapę, w której jako klucz podasz klienta, natomiast jako wartość
        przechowasz dług, który klient musi spłacić za niezapłacone zakupy.
        Dług stanowi różnica pomiędzy kwotą do zapłaty oraz gotówką, którą
        posiada klient.
     */
    public Map<Customer, BigDecimal> getCustomersAndDebts() {
        return customersWithProducts
                .entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> getDebt(entry.getKey(), entry.getValue())));
    }

    private BigDecimal getDebt(Customer customer, Map<Product, Long> products) {
        return customer.getCash().subtract(getExpense(products));
    }

}
