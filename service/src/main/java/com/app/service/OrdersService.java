package com.app.service;


import com.app.persistence.converter.CustomerWithProductsConverter;
import com.app.persistence.model.Category;
import com.app.persistence.model.Customer;
import com.app.persistence.model.CustomerWithProducts;
import com.app.persistence.model.Product;
import com.app.persistence.validator.CustomerWithProductsValidator;
import com.app.service.exception.OrdersServiceException;
import org.eclipse.collections.impl.collector.Collectors2;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.app.persistence.model.CustomerUtils.toAge;
import static com.app.persistence.model.ProductUtils.toCategory;
import static com.app.persistence.model.ProductUtils.toPrice;
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
    public List<Customer> getCustomersWithMaxExpense() {
        return customersWithProducts
                .entrySet()
                .stream()
                .collect(Collectors.groupingBy(
                        e -> totalPrice(e.getValue()),
                        Collectors.mapping(Map.Entry::getKey, Collectors.toList())
                ))
                .entrySet()
                .stream()
                .max(Map.Entry.comparingByKey())
                .orElseThrow()
                .getValue();
    }

    private BigDecimal totalPrice(Map<Product, Long> countedProducts) {
        return countedProducts
                .entrySet()
                .stream()
                .map(e -> toPrice.apply(e.getKey()).multiply(BigDecimal.valueOf(e.getValue())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static BigDecimal getTotalPrice(Map.Entry<Product, Long> entry) {
        return toPrice.apply(entry.getKey()).multiply(BigDecimal.valueOf(entry.getValue()));
    }

    /*
      W osobnej metodzie wyznacz klienta, który zapłacił najwięcej
      za zakupy z wybranej kategorii. Nazwę kategorii przekaż jako
      argument funkcji.
  */
    public Customer getCustomerWithMaxExpenseOnCategory(Category category) {
        if (category == null) {
            throw new OrdersServiceException("Category is null");
        }
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
    public Map<Integer, List<Category>> findMostPopularCategoryForAge() {
        return customersWithProducts
                .entrySet()
                .stream()
                .collect(Collectors.groupingBy(
                        e -> toAge.apply(e.getKey()),
                        // [Integer, Map[Cutomer, Map[Product, Long]]]
                        Collectors.collectingAndThen(
                                // ROBIMY LISTE PRODUKTOW DLA DANEGO WIEKU
                                Collectors.flatMapping(ee -> ee.getValue()
                                        .entrySet()
                                        .stream()
                                        .flatMap(eee -> Collections.nCopies(eee.getValue().intValue(), eee.getKey()).stream()), Collectors.toList()),
                                // GRUPUJEMY OTRZYMANA WYZEJ LISTE PRODUKTOW PO KATEGORII I WYCIAGAMY KATEGORIE NAJPOPULARNIEJSZA - MOZE BYC
                                // ICH KILKA
                                products -> products
                                        .stream()
                                        .collect(Collectors.groupingBy(toCategory, counting()))
                                        .entrySet()
                                        .stream()
                                        .collect(Collectors.groupingBy(Map.Entry::getValue, Collectors.mapping(Map.Entry::getKey, toList())))
                                        .entrySet()
                                        .stream()
                                        .max(Map.Entry.comparingByKey())
                                        .orElseThrow()
                                        .getValue()
                        )
                ));
    }
    // --- KM ---

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
                .collect(Collectors.groupingBy(entry -> toCategory.apply(entry.getKey())))
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
                .flatMap(e -> e.entrySet()
                        .stream()
                        .flatMap(ee -> Collections.nCopies(ee.getValue().intValue(), ee.getKey()).stream()))
                .collect(Collectors.groupingBy(
                        toCategory,
                        Collectors.collectingAndThen(
                                Collectors.mapping(toPrice, toList()),
                                prices -> prices
                                        .stream()
                                        .collect(Collectors2.summarizingBigDecimal(p -> p))
                                        .getAverage()
                        )
                ));
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
                .collect(Collectors.groupingBy(toCategory))
                .entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> getProductWithMaxPrice(entry.getValue())));
    }

    private static Product getProductWithMaxPrice(List<Product> products) {
        return products.stream()
                .max(Comparator.comparing(toPrice))
                .orElseThrow();
    }

    public Map<Category, Product> getCategoriesAndProductsWithMinPrice() {
        return customersWithProducts.values()
                .stream()
                .map(Map::keySet)
                .flatMap(Set::stream)
                .collect(Collectors.groupingBy(toCategory))
                .entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> getProductWithMinPrice(entry.getValue())));
    }

    private static Product getProductWithMinPrice(List<Product> products) {
        return products.stream()
                .min(Comparator.comparing(toPrice))
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
                .collect(Collectors.groupingBy(entry -> toCategory.apply(entry.getKey())))
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

    private static BigDecimal getExpense(Map<Product, Long> products) {
        return products.entrySet()
                .stream()
                .map(OrdersService::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

}
