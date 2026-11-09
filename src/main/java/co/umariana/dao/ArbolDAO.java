package co.umariana.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import co.umariana.conexion.ConexionBD;
import co.umariana.modelo.ArbolGenealogico;
import co.umariana.modelo.Persona;
import co.umariana.modelo.ResumenArbol;

/**
 * DAO (Data Access Object): habla con la base de datos para los árboles y miembros.
 **/
public class ArbolDAO {

    /** 
    // =====================================================================
    // ETAPA 4.1. Guardar un árbol nuevo (INSERT en dos tablas, con transacción)
    // =====================================================================
    */
    public long guardar(String nombre, ArbolGenealogico arbol) {
        if (nombre == null || nombre.trim().isEmpty() || nombre.length() > 100 || arbol == null) {
            return -1;
        }

        Connection con = null;
        try {
            con = ConexionBD.conectar();
            con.setAutoCommit(false);

            long arbolId = -1;
            String sqlArbol = "INSERT INTO arboles (nombre) VALUES (?)";
            try (PreparedStatement ps = con.prepareStatement(sqlArbol, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, nombre.trim());
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        arbolId = rs.getLong(1);
                    }
                }
            }

            if (arbolId == -1) {
                con.rollback();
                return -1;
            }

            String sqlMiembro = "INSERT INTO miembros (arbol_id, posicion, nombre, nacimiento, fallecimiento) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement ps = con.prepareStatement(sqlMiembro)) {
                for (Persona p : arbol.miembros()) {
                    ps.setLong(1, arbolId);
                    ps.setString(2, p.getPosicion());
                    ps.setString(3, p.getNombre());
                    ps.setInt(4, p.getNacimiento());
                    if (p.getFallecimiento() != null) {
                        ps.setInt(5, p.getFallecimiento());
                    } else {
                        ps.setNull(5, Types.INTEGER);
                    }
                    ps.addBatch();
                }
                ps.executeBatch();
            }

            con.commit();
            return arbolId;
        } catch (SQLException e) {
            System.out.println("Error al guardar árbol: " + e.getMessage());
            if (con != null) {
                try {
                    con.rollback();
                } catch (SQLException ex) {
                    System.out.println("Error en rollback: " + ex.getMessage());
                }
            }
            return -1;
        } finally {
            if (con != null) {
                try {
                    con.setAutoCommit(true);
                    con.close();
                } catch (SQLException e) {
                    System.out.println("Error al cerrar conexión: " + e.getMessage());
                }
            }
        }
    }
    /** 
    // =====================================================================
    // ETAPA 4.2. Listar (SELECT con JOIN y COUNT)
    // =====================================================================
    */

    public List<ResumenArbol> listar() {
        List<ResumenArbol> lista = new ArrayList<>();
        String sql = "SELECT a.id, a.nombre, a.creado, COUNT(m.id) AS total " +
                     "FROM arboles a LEFT JOIN miembros m ON m.arbol_id = a.id " +
                     "GROUP BY a.id, a.nombre, a.creado ORDER BY a.id";

        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                long id = rs.getLong("id");
                String nombre = rs.getString("nombre");
                LocalDateTime creado = rs.getTimestamp("creado").toLocalDateTime();
                int total = rs.getInt("total");
                lista.add(new ResumenArbol(id, nombre, total, creado));
            }
        } catch (SQLException e) {
            System.out.println("Error al listar árboles: " + e.getMessage());
            return new ArrayList<>();
        }

        return lista;
    }
    /** 
    // =====================================================================
    // ETAPA 4.3. Cargar un árbol (SELECT y reconstrucción del árbol)
    // =====================================================================
    */

    public ArbolGenealogico cargar(long id) {
        String sql = "SELECT posicion, nombre, nacimiento, fallecimiento FROM miembros WHERE arbol_id = ? ORDER BY id";
        List<Persona> personas = new ArrayList<>();

        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String pos = rs.getString("posicion");
                    String nom = rs.getString("nombre");
                    int nac = rs.getInt("nacimiento");
                    int fallVal = rs.getInt("fallecimiento");
                    Integer fall = rs.wasNull() ? null : fallVal;
                    personas.add(new Persona(pos, nom, nac, fall));
                }
            }
        } catch (SQLException e) {
            System.out.println("Error al cargar árbol: " + e.getMessage());
            return null;
        }

        if (personas.isEmpty()) {
            return null;
        }

        try {
            ArbolGenealogico arbol = new ArbolGenealogico(personas.get(0));
            for (int i = 1; i < personas.size(); i++) {
                arbol.agregar(personas.get(i));
            }
            return arbol;
        } catch (Exception e) {
            System.out.println("Error al reconstruir el árbol: " + e.getMessage());
            return null;
        }
    }
    /** 
    // =====================================================================
    // ETAPA 4.4. Actualizar (reemplazar los miembros de un árbol, con transacción)
    // =====================================================================
    */
    public boolean actualizar(long id, ArbolGenealogico arbol) {
        if (arbol == null) {
            return false;
        }

        Connection con = null;
        try {
            con = ConexionBD.conectar();
            con.setAutoCommit(false);

            String sqlExiste = "SELECT id FROM arboles WHERE id = ?";
            boolean existe = false;
            try (PreparedStatement ps = con.prepareStatement(sqlExiste)) {
                ps.setLong(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        existe = true;
                    }
                }
            }

            if (!existe) {
                con.rollback();
                return false;
            }

            String sqlDelete = "DELETE FROM miembros WHERE arbol_id = ?";
            try (PreparedStatement ps = con.prepareStatement(sqlDelete)) {
                ps.setLong(1, id);
                ps.executeUpdate();
            }

            String sqlInsert = "INSERT INTO miembros (arbol_id, posicion, nombre, nacimiento, fallecimiento) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement ps = con.prepareStatement(sqlInsert)) {
                for (Persona p : arbol.miembros()) {
                    ps.setLong(1, id);
                    ps.setString(2, p.getPosicion());
                    ps.setString(3, p.getNombre());
                    ps.setInt(4, p.getNacimiento());
                    if (p.getFallecimiento() != null) {
                        ps.setInt(5, p.getFallecimiento());
                    } else {
                        ps.setNull(5, Types.INTEGER);
                    }
                    ps.addBatch();
                }
                ps.executeBatch();
            }

            con.commit();
            return true;
        } catch (SQLException e) {
            System.out.println("Error al actualizar árbol: " + e.getMessage());
            if (con != null) {
                try {
                    con.rollback();
                } catch (SQLException ex) {
                    System.out.println("Error en rollback: " + ex.getMessage());
                }
            }
            return false;
        } finally {
            if (con != null) {
                try {
                    con.setAutoCommit(true);
                    con.close();
                } catch (SQLException e) {
                    System.out.println("Error al cerrar conexión: " + e.getMessage());
                }
            }
        }
    }

    /** 
    // =====================================================================
    // ETAPA 4.5. Eliminar (DELETE)
    // =====================================================================
    */
   
    public boolean eliminar(long id) {
        String sql = "DELETE FROM arboles WHERE id = ?";
        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, id);
            int filasAfectadas = ps.executeUpdate();
            return filasAfectadas > 0;
        } catch (SQLException e) {
            System.out.println("Error al eliminar árbol: " + e.getMessage());
            return false;
        }
    }

    public boolean probarConexion() {
        try (Connection con = ConexionBD.conectar()) {
            return con != null && !con.isClosed();
        } catch (Exception e) {
            return false;
        }
    }
}
