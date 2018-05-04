package org.vaadin.crudui.app;

import java.util.List;

/**
 * @author Alejandro Duarte
 */
public class TeamRepository {

    public static List<Team> findAll() {
        return JPAService.runInTransaction(em ->
                em.createQuery("select t from Team t").getResultList()
        );
    }

    public static Team save(Team team) {
        return JPAService.runInTransaction(em -> em.merge(team));
    }

}
