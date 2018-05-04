package org.vaadin.crudui.app;

import java.util.List;

/**
 * @author Alejandro Duarte
 */
public class ProductRepository {

    public static List<Product> findAll() {
        return JPAService.runInTransaction(em ->
                em.createQuery("select p from Product p").getResultList()
        );
    }

    public static Product save(Product product) {
        return JPAService.runInTransaction(em -> em.merge(product));
    }

}
