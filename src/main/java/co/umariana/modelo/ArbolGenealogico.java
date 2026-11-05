package co.umariana.modelo;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * El árbol genealógico: un SUJETO (posición "1") con sus ASCENDIENTES (padres, abuelos... en Ahnentafel)
 * y sus DESCENDIENTES (hijos, nietos... en d'Aboville). Ver la clase Posicion.
 */
public class ArbolGenealogico {

    private static class Nodo {
        Persona persona;
        Nodo padre; // Ahnentafel 2n
        Nodo madre; // Ahnentafel 2n + 1
        List<Nodo> hijos = new ArrayList<>(); // d'Aboville

        Nodo(Persona persona) {
            this.persona = persona;
        }
    }

    private final Nodo raiz;
    private int tamano;

    /**
     * Crea un árbol que solo tiene al sujeto.
     */
    public ArbolGenealogico(Persona sujeto) {
        if (sujeto == null) {
            throw new IllegalArgumentException("El sujeto no puede ser nulo.");
        }
        sujeto.validar();
        if (!Posicion.esSujeto(sujeto.getPosicion())) {
            throw new ArbolInvalidoException("La posición del sujeto debe ser '1'.");
        }
        this.raiz = new Nodo(sujeto);
        this.tamano = 1;
    }

    /** @return el sujeto (la persona de la posición "1") */
    public Persona sujeto() {
        return raiz.persona;
    }

    /** @return cuántos miembros tiene el árbol */
    public int tamano() {
        return tamano;
    }

    /**
     * @return la persona de esa posición, o null si no existe en el árbol o si la posición no es válida
     */
    public Persona buscar(String posicion) {
        Nodo nodo = buscarNodo(posicion);
        return (nodo != null) ? nodo.persona : null;
    }

    /** @return true si hay alguien en esa posición */
    public boolean contiene(String posicion) {
        return buscarNodo(posicion) != null;
    }

    private Nodo buscarNodo(String posicion) {
        if (!Posicion.esValida(posicion)) return null;
        if (Posicion.esSujeto(posicion)) return raiz;

        if (Posicion.esAhnentafel(posicion)) {
            int n = Integer.parseInt(posicion);
            // Convertir a bits para seguir el camino desde la raíz
            String binary = Integer.toBinaryString(n);
            Nodo actual = raiz;
            for (int i = 1; i < binary.length(); i++) {
                if (actual == null) return null;
                if (binary.charAt(i) == '0') {
                    actual = actual.padre;
                } else {
                    actual = actual.madre;
                }
            }
            return actual;
        } else if (Posicion.esDAboville(posicion)) {
            String[] partes = posicion.split("\\.");
            Nodo actual = raiz;
            for (int i = 1; i < partes.length; i++) {
                if (actual == null) return null;
                int ordenBuscado = Integer.parseInt(partes[i]);
                Nodo siguiente = null;
                for (Nodo hijo : actual.hijos) {
                    if (Posicion.orden(hijo.persona.getPosicion()) == ordenBuscado) {
                        siguiente = hijo;
                        break;
                    }
                }
                actual = siguiente;
            }
            return actual;
        }
        return null;
    }

    /**
     * Agrega una persona en la posición que ella misma trae.
     */
    public Persona agregar(Persona persona) {
        if (persona == null) {
            throw new IllegalArgumentException("La persona no puede ser nula.");
        }
        persona.validar();

        String pos = persona.getPosicion();
        if (Posicion.esSujeto(pos)) {
            throw new ArbolInvalidoException("El sujeto (posición 1) ya existe.");
        }
        if (contiene(pos)) {
            throw new ArbolInvalidoException("La posición " + pos + " ya existe en el árbol.");
        }

        if (Posicion.esAscendiente(pos)) {
            String posHijo = Posicion.posicionDelHijoEnLinea(pos);
            Nodo nodoHijo = buscarNodo(posHijo);
            if (nodoHijo == null) {
                throw new ArbolInvalidoException("No existe el hijo/a en la posición " + posHijo + " para agregar el ascendiente.");
            }
            if (persona.getNacimiento() >= nodoHijo.persona.getNacimiento()) {
                throw new ArbolInvalidoException("El ascendiente debe haber nacido antes que su hijo (" + persona.getNacimiento() + " >= " + nodoHijo.persona.getNacimiento() + ").");
            }

            int n = Integer.parseInt(pos);
            Nodo nuevoNodo = new Nodo(persona);
            if (n % 2 == 0) {
                nodoHijo.padre = nuevoNodo;
            } else {
                nodoHijo.madre = nuevoNodo;
            }
            tamano++;
            return persona;
        } else if (Posicion.esDescendiente(pos)) {
            String posProgenitor = Posicion.posicionDelProgenitor(pos);
            Nodo nodoProgenitor = buscarNodo(posProgenitor);
            if (nodoProgenitor == null) {
                throw new ArbolInvalidoException("No existe el progenitor en la posición " + posProgenitor + " para agregar el descendiente.");
            }
            if (persona.getNacimiento() <= nodoProgenitor.persona.getNacimiento()) {
                throw new ArbolInvalidoException("El descendiente debe haber nacido después que su progenitor (" + persona.getNacimiento() + " <= " + nodoProgenitor.persona.getNacimiento() + ").");
            }

            Nodo nuevoNodo = new Nodo(persona);
            nodoProgenitor.hijos.add(nuevoNodo);
            // Mantener ordenada la lista de hijos por su orden d'Aboville
            nodoProgenitor.hijos.sort(Comparator.comparingInt(h -> Posicion.orden(h.persona.getPosicion())));
            tamano++;
            return persona;
        }

        throw new ArbolInvalidoException("Posición no válida para agregar al árbol: " + pos);
    }

    public Persona agregarPadre(String posicion, String nombre, int nacimiento, Integer fallecimiento) {
        if (!contiene(posicion)) {
            throw new ArbolInvalidoException("No existe la persona en la posición " + posicion);
        }
        if (!Posicion.esSujeto(posicion) && !Posicion.esAscendiente(posicion)) {
            throw new ArbolInvalidoException("Solo se pueden agregar padres al sujeto y a sus ascendientes.");
        }
        String posPadre = Posicion.posicionDelPadre(posicion);
        Persona padre = new Persona(posPadre, nombre, nacimiento, fallecimiento);
        return agregar(padre);
    }

    public Persona agregarMadre(String posicion, String nombre, int nacimiento, Integer fallecimiento) {
        if (!contiene(posicion)) {
            throw new ArbolInvalidoException("No existe la persona en la posición " + posicion);
        }
        if (!Posicion.esSujeto(posicion) && !Posicion.esAscendiente(posicion)) {
            throw new ArbolInvalidoException("Solo se pueden agregar madres al sujeto y a sus ascendientes.");
        }
        String posMadre = Posicion.posicionDeLaMadre(posicion);
        Persona madre = new Persona(posMadre, nombre, nacimiento, fallecimiento);
        return agregar(madre);
    }

    public Persona agregarHijo(String posicion, String nombre, int nacimiento, Integer fallecimiento) {
        Nodo nodoProgenitor = buscarNodo(posicion);
        if (nodoProgenitor == null) {
            throw new ArbolInvalidoException("No existe la persona en la posición " + posicion);
        }
        if (!Posicion.esSujeto(posicion) && !Posicion.esDescendiente(posicion)) {
            throw new ArbolInvalidoException("Solo se pueden agregar hijos al sujeto y a sus descendientes.");
        }

        int maxOrden = 0;
        for (Nodo hijo : nodoProgenitor.hijos) {
            int ord = Posicion.orden(hijo.persona.getPosicion());
            if (ord > maxOrden) {
                maxOrden = ord;
            }
        }
        String posHijo = posicion + "." + (maxOrden + 1);
        Persona hijo = new Persona(posHijo, nombre, nacimiento, fallecimiento);
        return agregar(hijo);
    }

    public int eliminar(String posicion) {
        if (Posicion.esSujeto(posicion)) {
            throw new ArbolInvalidoException("El sujeto (posición 1) no se puede eliminar.");
        }
        Nodo nodo = buscarNodo(posicion);
        if (nodo == null) {
            throw new ArbolInvalidoException("No existe la persona en la posición " + posicion);
        }

        int eliminados = contarSubarbol(nodo);

        if (Posicion.esAscendiente(posicion)) {
            String posHijo = Posicion.posicionDelHijoEnLinea(posicion);
            Nodo nodoHijo = buscarNodo(posHijo);
            if (nodoHijo != null) {
                int n = Integer.parseInt(posicion);
                if (n % 2 == 0) {
                    nodoHijo.padre = null;
                } else {
                    nodoHijo.madre = null;
                }
            }
        } else if (Posicion.esDescendiente(posicion)) {
            String posProgenitor = Posicion.posicionDelProgenitor(posicion);
            Nodo nodoProgenitor = buscarNodo(posProgenitor);
            if (nodoProgenitor != null) {
                nodoProgenitor.hijos.removeIf(h -> h.persona.getPosicion().equals(posicion));
            }
        }

        tamano -= eliminados;
        return eliminados;
    }

    private int contarSubarbol(Nodo n) {
        if (n == null) return 0;
        int c = 1;
        c += contarSubarbol(n.padre);
        c += contarSubarbol(n.madre);
        for (Nodo h : n.hijos) {
            c += contarSubarbol(h);
        }
        return c;
    }

    public List<Persona> miembros() {
        List<Persona> lista = new ArrayList<>();
        lista.add(raiz.persona);
        lista.addAll(ascendientes());
        lista.addAll(descendientes());
        return lista;
    }

    public List<Persona> ascendientes() {
        List<Persona> lista = new ArrayList<>();
        recolectarAscendientes(raiz, lista);
        lista.sort(Comparator.comparingInt(p -> Integer.parseInt(p.getPosicion())));
        return lista;
    }

    private void recolectarAscendientes(Nodo actual, List<Persona> lista) {
        if (actual == null) return;
        if (actual.padre != null) {
            lista.add(actual.padre.persona);
            recolectarAscendientes(actual.padre, lista);
        }
        if (actual.madre != null) {
            lista.add(actual.madre.persona);
            recolectarAscendientes(actual.madre, lista);
        }
    }

    public List<Persona> descendientes() {
        List<Persona> lista = new ArrayList<>();
        recolectarDescendientesPreorden(raiz, lista);
        return lista;
    }

    private void recolectarDescendientesPreorden(Nodo actual, List<Persona> lista) {
        if (actual == null) return;
        for (Nodo hijo : actual.hijos) {
            lista.add(hijo.persona);
            recolectarDescendientesPreorden(hijo, lista);
        }
    }
}