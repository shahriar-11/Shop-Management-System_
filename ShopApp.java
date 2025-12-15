// Shop Management System
// Java Swing + MySQL
// Author: Student Project

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;

public class ShopApp extends JFrame {

    private static final String URL = "jdbc:mysql://localhost:3306/shop_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static final String USER = "root";
    private static final String PASS = ""; 

    DefaultTableModel prodModel, cartModel;
    JTable prodTable, cartTable;
    JComboBox<String> billingSearch;
    JLabel totalLabel;

    public ShopApp() {
        setTitle("Shop Management System");
        setSize(1250, 650);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        initDB();
        initUI();
        refreshProducts();
    }

    private void initDB() {
        try (Connection c = DriverManager.getConnection("jdbc:mysql://localhost:3306/?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC", USER, PASS);
             Statement s = c.createStatement()) {
            s.execute("CREATE DATABASE IF NOT EXISTS shop_db");
        } catch (Exception ignored) {}

        try (Connection c = DriverManager.getConnection(URL, USER, PASS);
             Statement s = c.createStatement()) {

            s.execute("CREATE TABLE IF NOT EXISTS products (" +
                    "product_id INT AUTO_INCREMENT PRIMARY KEY," +
                    "name VARCHAR(100) NOT NULL," +
                    "quantity INT NOT NULL," +
                    "price DOUBLE NOT NULL)");

            s.execute("CREATE TABLE IF NOT EXISTS sales (" +
                    "sale_id INT AUTO_INCREMENT PRIMARY KEY," +
                    "sale_date DATETIME DEFAULT CURRENT_TIMESTAMP," +
                    "total DOUBLE NOT NULL)");

            s.execute("CREATE TABLE IF NOT EXISTS sales_items (" +
                    "item_id INT AUTO_INCREMENT PRIMARY KEY," +
                    "sale_id INT NOT NULL," +
                    "product_id INT NOT NULL," +
                    "quantity INT NOT NULL," +
                    "price DOUBLE NOT NULL," +
                    "total DOUBLE NOT NULL," +
                    "FOREIGN KEY (sale_id) REFERENCES sales(sale_id)," +
                    "FOREIGN KEY (product_id) REFERENCES products(product_id))");

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void initUI() {
        JPanel left = new JPanel(new BorderLayout(5,5));
        left.setPreferredSize(new Dimension(520,0));

        JPanel addPanel = new JPanel(new GridLayout(5,2,5,5));
        addPanel.setBorder(BorderFactory.createTitledBorder("Add / Update Product"));

        JTextField nameF = new JTextField();
        JTextField qtyF = new JTextField();
        JTextField priceF = new JTextField();
        JButton addBtn = new JButton("Add Product");
        JButton updateBtn = new JButton("Update Selected");
        JButton deleteBtn = new JButton("Delete Selected");

        addPanel.add(new JLabel("Name")); addPanel.add(nameF);
        addPanel.add(new JLabel("Qty")); addPanel.add(qtyF);
        addPanel.add(new JLabel("Price")); addPanel.add(priceF);
        addPanel.add(addBtn); addPanel.add(updateBtn);

        left.add(addPanel, BorderLayout.NORTH);

        prodModel = new DefaultTableModel(new String[]{"ID","Name","Qty","Price"},0) {
            public boolean isCellEditable(int row, int column){ return false; }
        };
        prodTable = new JTable(prodModel);
        prodTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        left.add(new JScrollPane(prodTable), BorderLayout.CENTER);

        JPanel deletePanel = new JPanel();
        deletePanel.add(deleteBtn);
        left.add(deletePanel, BorderLayout.SOUTH);

        add(left, BorderLayout.WEST);

        JPanel right = new JPanel(new BorderLayout(5,5));
        right.setBorder(BorderFactory.createTitledBorder("Customer Billing"));

        JPanel billTop = new JPanel();
        billingSearch = new JComboBox<>();
        billingSearch.setPreferredSize(new Dimension(300,25));
        enableProductSearch(billingSearch);

        JTextField billQty = new JTextField(5);
        JButton addCart = new JButton("Add To Cart");

        billTop.add(new JLabel("Search Product"));
        billTop.add(billingSearch);
        billTop.add(new JLabel("Qty"));
        billTop.add(billQty);
        billTop.add(addCart);

        right.add(billTop, BorderLayout.NORTH);

        cartModel = new DefaultTableModel(new String[]{"ID","Name","Qty","Price","Total"},0) {
            public boolean isCellEditable(int row, int column){ return false; }
        };
        cartTable = new JTable(cartModel);
        right.add(new JScrollPane(cartTable), BorderLayout.CENTER);

        JPanel cartBottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton removeItem = new JButton("Remove Selected");
        JButton clearCart = new JButton("Clear Cart");
        JButton finalizeBill = new JButton("Finalize Bill");
        totalLabel = new JLabel("Total: 0.00");
        totalLabel.setFont(new Font("Arial",Font.BOLD,16));

        cartBottom.add(removeItem);
        cartBottom.add(clearCart);
        cartBottom.add(finalizeBill);
        cartBottom.add(totalLabel);

        right.add(cartBottom, BorderLayout.SOUTH);

        add(right, BorderLayout.CENTER);

        Runnable updateTotal = () -> {
            double total = 0;
            for(int i = 0; i < cartModel.getRowCount(); i++) {
                total += (double)cartModel.getValueAt(i, 4);
            }
            totalLabel.setText(String.format("Total: %.2f", total));
        };

        addBtn.addActionListener(e -> {
            String name = nameF.getText().trim();
            String qtyText = qtyF.getText().trim();
            String priceText = priceF.getText().trim();

            if(name.isEmpty() || qtyText.isEmpty() || priceText.isEmpty()) {
                JOptionPane.showMessageDialog(this,"Fill all fields");
                return;
            }

            try {
                int qty = Integer.parseInt(qtyText);
                double price = Double.parseDouble(priceText);

                if(qty <= 0 || price <= 0) {
                    JOptionPane.showMessageDialog(this,"Quantity and Price must be > 0");
                    return;
                }

                try(Connection c = DriverManager.getConnection(URL,USER,PASS);
                    PreparedStatement ps = c.prepareStatement(
                            "INSERT INTO products(name,quantity,price) VALUES(?,?,?)")) {
                    ps.setString(1,name);
                    ps.setInt(2,qty);
                    ps.setDouble(3,price);
                    ps.executeUpdate();
                    JOptionPane.showMessageDialog(this,"Product added successfully!");
                    nameF.setText(""); qtyF.setText(""); priceF.setText("");
                    refreshProducts();
                }
            } catch(NumberFormatException ex) {
                JOptionPane.showMessageDialog(this,"Enter valid number for Qty and Price");
            } catch(Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this,"Database error: " + ex.getMessage());
            }
        });
    }

    private void enableProductSearch(JComboBox<String> combo){
        combo.setEditable(true);
        JTextField tf=(JTextField)combo.getEditor().getEditorComponent();
        tf.addKeyListener(new KeyAdapter(){
            public void keyReleased(KeyEvent e){
                String text=tf.getText();
                combo.removeAllItems();
                try(Connection c=DriverManager.getConnection(URL,USER,PASS);
                    PreparedStatement ps=c.prepareStatement("SELECT * FROM products WHERE name LIKE ?")) {
                    ps.setString(1,"%"+text+"%");
                    ResultSet rs=ps.executeQuery();
                    while(rs.next()){
                        combo.addItem(rs.getInt("product_id")+" - "+rs.getString("name")+" - "+rs.getInt("quantity")+" - "+rs.getDouble("price"));
                    }
                }catch(Exception ex){ ex.printStackTrace(); }
                tf.setText(text); combo.showPopup();
            }
        });
    }

    private void refreshProducts(){
        prodModel.setRowCount(0);
        try(Connection c=DriverManager.getConnection(URL,USER,PASS);
            Statement s=c.createStatement();
            ResultSet rs=s.executeQuery("SELECT * FROM products")){
            while(rs.next()){
                prodModel.addRow(new Object[]{rs.getInt("product_id"),rs.getString("name"),rs.getInt("quantity"),rs.getDouble("price")});
            }
        } catch(Exception ex){ ex.printStackTrace(); }
    }

    public static void main(String[] args){
        try{ Class.forName("com.mysql.cj.jdbc.Driver"); } catch(Exception ignored){}
        SwingUtilities.invokeLater(() -> new ShopApp().setVisible(true));
    }
}
