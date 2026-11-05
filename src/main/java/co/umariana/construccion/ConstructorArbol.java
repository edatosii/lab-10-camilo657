package co.umariana.construccion;

import co.umariana.modelo.ArbolGenealogico;
import co.umariana.modelo.ArbolInvalidoException;
import co.umariana.modelo.Persona;
import co.umariana.modelo.Posicion;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Construye un árbol genealógico a partir de un texto: cada línea es una persona.
 *   posicion;nombre;nacimiento;fallecimiento
 */
public final class ConstructorArbol {

    private ConstructorArbol() {
    }

    private static class LineaPersona {
        int numLinea;
        Persona persona;

        LineaPersona(int numLinea, Persona persona) {
            this.numLinea = numLinea;
            this.persona = persona;
        }
    }

    /**
     * Convierte el texto en un árbol.
     *
     * @throws ArbolInvalidoException si el texto no cumple las reglas
     */
    public static ArbolGenealogico construir(String texto) {
        if (texto == null || texto.trim().isEmpty()) {
            throw new ArbolInvalidoException("El texto está vacío.");
        }

        String[] lineas = texto.split("\r?\n", -1);
        List<LineaPersona> candidatas = new ArrayList<>();
        Set<String> posicionesVistas = new HashSet<>();
        boolean tieneLineasValidas = false;

        for (int i = 0; i < lineas.length; i++) {
            int numLinea = i + 1;
            String linea = lineas[i].trim();
            if (linea.isEmpty()) {
                continue;
            }
            tieneLineasValidas = true;

            String[] partes = linea.split(";", -1);
            if (partes.length < 3 || partes.length > 4) {
                throw new ArbolInvalidoException("Línea " + numLinea + ": formato incorrecto de campos.");
            }

            String posStr = partes[0].trim();
            String nombre = partes[1].trim();
            String nacStr = partes[2].trim();
            String fallStr = (partes.length == 4) ? partes[3].trim() : "";

            // REGLA 5: Verificar posiciones duplicadas en el texto
            if (posicionesVistas.contains(posStr)) {
                throw new ArbolInvalidoException("Línea " + numLinea + ": la posición " + posStr + " está repetida.");
            }
            posicionesVistas.add(posStr);

            int nacimiento;
            try {
                nacimiento = Integer.parseInt(nacStr);
            } catch (NumberFormatException e) {
                throw new ArbolInvalidoException("Línea " + numLinea + ": año de nacimiento no válido.");
            }

            Integer fallecimiento = null;
            if (!fallStr.isEmpty()) {
                try {
                    fallecimiento = Integer.parseInt(fallStr);
                } catch (NumberFormatException e) {
                    throw new ArbolInvalidoException("Línea " + numLinea + ": año de fallecimiento no válido.");
                }
            }

            Persona persona = new Persona(posStr, nombre, nacimiento, fallecimiento);
            try {
                persona.validar();
            } catch (IllegalArgumentException e) {
                throw new ArbolInvalidoException("Línea " + numLinea + ": " + e.getMessage());
            }

            candidatas.add(new LineaPersona(numLinea, persona));
        }

        if (!tieneLineasValidas) {
            throw new ArbolInvalidoException("El texto está vacío.");
        }

        // REGLA 6: Buscar el sujeto (posición "1")
        LineaPersona sujetoLinea = null;
        for (LineaPersona lp : candidatas) {
            if (Posicion.esSujeto(lp.persona.getPosicion())) {
                sujetoLinea = lp;
                break;
            }
        }

        if (sujetoLinea == null) {
            throw new ArbolInvalidoException("Falta la persona del sujeto (posición 1).");
        }

        // Crear el árbol con el sujeto
        ArbolGenealogico arbol = new ArbolGenealogico(sujetoLinea.persona);

        // Separar el resto de personas
        List<LineaPersona> resto = new ArrayList<>();
        for (LineaPersona lp : candidatas) {
            if (lp != sujetoLinea) {
                resto.add(lp);
            }
        }

        // REGLA 7: Ordenar para agregar en orden de dependencia
        resto.sort(new Comparator<LineaPersona>() {
            @Override
            public int compare(LineaPersona o1, LineaPersona o2) {
                String p1 = o1.persona.getPosicion();
                String p2 = o2.persona.getPosicion();

                boolean asc1 = Posicion.esAscendiente(p1);
                boolean asc2 = Posicion.esAscendiente(p2);

                if (asc1 && asc2) {
                    return Integer.compare(Integer.parseInt(p1), Integer.parseInt(p2));
                }
                if (asc1 && !asc2) {
                    return -1;
                }
                if (!asc1 && asc2) {
                    return 1;
                }

                // Ambos son descendientes d'Aboville: comparar por generación (puntos)
                int gen1 = Posicion.generacion(p1);
                int gen2 = Posicion.generacion(p2);
                if (gen1 != gen2) {
                    return Integer.compare(gen1, gen2);
                }

                // Si tienen la misma generación, comparar nivel por nivel numéricamente
                String[] partes1 = p1.split("\\.");
                String[] partes2 = p2.split("\\.");
                for (int i = 0; i < partes1.length; i++) {
                    int num1 = Integer.parseInt(partes1[i]);
                    int num2 = Integer.parseInt(partes2[i]);
                    if (num1 != num2) {
                        return Integer.compare(num1, num2);
                    }
                }
                return 0;
            }
        });

        // Insertar en el árbol
        for (LineaPersona lp : resto) {
            try {
                arbol.agregar(lp.persona);
            } catch (ArbolInvalidoException e) {
                throw new ArbolInvalidoException("Línea " + lp.numLinea + ": " + e.getMessage());
            }
        }

        return arbol;
    }
}