package org.vaadin.crudui.app;

import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.DateField;
import com.vaadin.ui.Grid;
import com.vaadin.ui.Notification;
import com.vaadin.ui.PasswordField;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.renderers.DateRenderer;
import com.vaadin.ui.renderers.TextRenderer;
import org.apache.bval.util.StringUtils;
import org.vaadin.crudui.crud.Crud;
import org.vaadin.crudui.crud.CrudListener;
import org.vaadin.crudui.crud.CrudOperation;
import org.vaadin.crudui.crud.impl.EditableGridCrud;
import org.vaadin.crudui.crud.impl.GridCrud;
import org.vaadin.crudui.form.impl.field.provider.CheckBoxGroupProvider;
import org.vaadin.crudui.form.impl.field.provider.ComboBoxProvider;
import org.vaadin.crudui.form.impl.form.factory.GridLayoutCrudFormFactory;
import org.vaadin.crudui.layout.impl.HorizontalSplitCrudLayout;
import org.vaadin.jetty.VaadinJettyServer;

import java.sql.Date;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Alejandro Duarte
 */
public class TestUI extends UI implements CrudListener<User> {

    public static void main(String[] args) throws Exception {
        JPAService.init();
        VaadinJettyServer server = new VaadinJettyServer(9090, TestUI.class);
        server.start();
    }

    private TabSheet tabSheet = new TabSheet();

    @Override
    protected void init(VaadinRequest request) {
        tabSheet.setSizeFull();
        setContent(tabSheet);

        addCrud(getDefaultCrud(), "Default");
        addCrud(getDefaultCrudWithFixes(), "Default (with fixes)");
        addCrud(getConfiguredCrud(), "Configured");
        addCrud(getEditableGridCrud(), "Editable Grid");
    }

    private void addCrud(Crud crud, String caption) {
        VerticalLayout layout = new VerticalLayout(crud);
        layout.setSizeFull();
        layout.setMargin(true);
        tabSheet.addTab(layout, caption);
    }

    private Crud getDefaultCrud() {
        return new GridCrud<>(User.class, this);
    }

    private Crud getDefaultCrudWithFixes() {
        GridCrud<User> crud = new GridCrud<>(User.class);
        crud.setCrudListener(this);
        crud.getCrudFormFactory().setFieldProvider("products", new CheckBoxGroupProvider<>(ProductRepository.findAll()));
        crud.getCrudFormFactory().setFieldProvider("mainProduct", new ComboBoxProvider<>(ProductRepository.findAll()));
        crud.getCrudFormFactory().setFieldProvider("userTeam", new ComboBoxProvider<>(TeamRepository.findAll()));

        return crud;
    }

    private Crud getConfiguredCrud() {
        GridCrud<User> crud = new GridCrud<>(User.class, new HorizontalSplitCrudLayout());
        crud.setCrudListener(this);

        GridLayoutCrudFormFactory<User> formFactory = new GridLayoutCrudFormFactory<>(User.class, 2, 2);
        crud.setCrudFormFactory(formFactory);

        formFactory.setUseBeanValidation(true);

        formFactory.setErrorListener(e -> Notification.show("Custom error message (simulated error)", Notification.Type.ERROR_MESSAGE));

        formFactory.setVisibleProperties(CrudOperation.READ, "id", "nom", "dateCreation", "email", "matricule", "products", "active", "mainProduct","userTeam");
        formFactory.setVisibleProperties(CrudOperation.ADD, "nom", "dateCreation", "email", "matricule", "products", "password", "mainProduct","userTeam", "active");
        formFactory.setVisibleProperties(CrudOperation.UPDATE, "id", "nom", "dateCreation", "gender","email", "matricule", "password", "products", "active", "mainProduct","userTeam");
        formFactory.setVisibleProperties(CrudOperation.DELETE, "nom", "email", "matricule");

        formFactory.setDisabledProperties("id");

        crud.getGrid().setColumns("nom", "dateCreation", "email", "matricule", "mainProduct","userTeam", "active");
        crud.getGrid().getColumn("mainProduct").setRenderer(group -> group == null ? "" : ((Product) group).getName(), new TextRenderer());
        crud.getGrid().getColumn("userTeam").setRenderer(team -> team == null ? "" : ((Team) team).getName(), new TextRenderer());
        ((Grid.Column<User, Date>) crud.getGrid().getColumn("dateCreation")).setRenderer(new DateRenderer("%1$tY-%1$tm-%1$te"));

        formFactory.setFieldType("password", PasswordField.class);
        formFactory.setFieldCreationListener("dateCreation", field -> ((DateField) field).setDateFormat("yyyy-MM-dd"));

        formFactory.setFieldProvider("products", new CheckBoxGroupProvider<>("Produit", ProductRepository.findAll(), Product::getName));
        formFactory.setFieldProvider("mainProduct", new ComboBoxProvider<>("Produit Principal", ProductRepository.findAll(), Product::getName));
        formFactory.setFieldProvider("userTeam", new ComboBoxProvider<>("Team", TeamRepository.findAll(), Team::getName));

        formFactory.setButtonCaption(CrudOperation.ADD, "Add new user");
        crud.setRowCountCaption("%d user(s) found");

        crud.setClickRowToUpdate(true);
        crud.setUpdateOperationVisible(false);

        return crud;
    }

    private Crud getEditableGridCrud() {
        EditableGridCrud<User> crud = new EditableGridCrud<>(User.class, this);

        crud.getGrid().setColumns("nom", "dateCreation", "email", "matricule", "password", "products", "mainProduct","userTeam", "active");
        crud.getCrudFormFactory().setVisibleProperties("nom", "dateCreation", "email", "matricule", "password", "products", "mainProduct","userTeam", "active");

        crud.getGrid().getColumn("password").setRenderer(user -> "********", new TextRenderer());
        crud.getGrid().getColumn("mainProduct").setRenderer(group -> group == null ? "" : ((Product) group).getName(), new TextRenderer());
        crud.getGrid().getColumn("userTeam").setRenderer(team -> team == null ? "" : ((Team) team).getName(), new TextRenderer());
        crud.getGrid().getColumn("products").setRenderer(products -> StringUtils.join(((Set<Product>) products).stream().map(g -> g.getName()).collect(Collectors.toList()), ", "), new TextRenderer());

        crud.getCrudFormFactory().setFieldType("password", PasswordField.class);
        crud.getCrudFormFactory().setFieldProvider("products", new CheckBoxGroupProvider<>(null, ProductRepository.findAll(), group -> group.getName()));
        crud.getCrudFormFactory().setFieldProvider("mainProduct", new ComboBoxProvider<>(null, ProductRepository.findAll(), group -> group.getName()));
        crud.getCrudFormFactory().setFieldProvider("userTeam", new ComboBoxProvider<>(null, TeamRepository.findAll(), team -> team.getName()));

        crud.getCrudFormFactory().setUseBeanValidation(true);

        return crud;
    }

    @Override
    public User add(User user) {
        UserRepository.save(user);
        return user;
    }

    @Override
    public User update(User user) {
        if (user.getId().equals(5l)) {
            throw new RuntimeException("A simulated error has occurred");
        }
        return UserRepository.save(user);
    }

    @Override
    public void delete(User user) {
        UserRepository.delete(user);
    }

    @Override
    public Collection<User> findAll() {
        return UserRepository.findAll();
    }

}
