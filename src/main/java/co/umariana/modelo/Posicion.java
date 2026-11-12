package co.umariana.modelo;

/**
 * Las posiciones de un árbol genealógico. Cada miembro tiene una posición (texto) que dice quién es
 * respecto al SUJETO (la persona de la que trata el árbol). Se usan dos sistemas:
 *
 * AHNENTAFEL (para los ASCENDIENTES: padres, abuelos...). Es un número entero:
 *
 *     1                  el sujeto
 *     2 = padre del 1    3 = madre del 1
 *     4 = padre del 2    5 = madre del 2    6 = padre del 3    7 = madre del 3
 *
 *   Regla: el padre de la persona n es 2n y la madre es 2n + 1. Por eso los números pares son
 *   padres y los impares (mayores que 1) son madres. Y la persona n es hijo/a de n / 2 (división entera).
 *
 * D'ABOVILLE (para los DESCENDIENTES: hijos, nietos...). Es el "1" del sujeto seguido de un punto y
 * el número de orden en cada generación:
 *
 *     1.1  primer hijo del sujeto      1.2  segundo hijo del sujeto
 *     1.1.1  primer hijo del 1.1       1.2.3  tercer hijo del 1.2
 *
 *   Regla: el progenitor de 1.2.3 es 1.2 (se quita el último número).
 *
 * La posición "1" (el sujeto) cuenta como Ahnentafel.
 *
 * LÍMITES (para que las posiciones quepan en la base de datos y en un int):
 *   - Ahnentafel: de 1 a 2047 (hasta 10 generaciones de ascendientes).
 *   - D'Aboville: cada número de orden va de 1 a 9999 y la posición completa tiene máximo 50 caracteres.
 */
public final class Posicion {

    /** La posición del sujeto. */
    public static final String SUJETO = "1";
    /** Mayor número Ahnentafel permitido (10 generaciones de ascendientes). */
    public static final int MAX_AHNENTAFEL = 2047;
    /** Mayor número de orden permitido en cada nivel d'Aboville. */
    public static final int MAX_ORDEN = 9999;
    /** Longitud máxima de una posición d'Aboville (es lo que cabe en la base de datos). */
    public static final int MAX_LONGITUD = 50;

    private Posicion() {
    }

    /**
     * @return true solo si la posición es exactamente "1"
     */
    public static boolean esSujeto(String posicion) {
        return SUJETO.equals(posicion);
    }

    /**
     * Ahnentafel: un entero positivo SIN ceros a la izquierda, de 1 a MAX_AHNENTAFEL, sin signo,
     * espacios ni puntos. Incluye el "1" del sujeto.
     */
    public static boolean esAhnentafel(String posicion) {
        if (posicion == null) return false;
        // Debe ser [1-9] seguido opcionalmente de digitos (sin ceros a la izquierda)
        if (!posicion.matches("^[1-9]\\d*$")) return false;
        try {
            int num = Integer.parseInt(posicion);
            return num >= 1 && num <= MAX_AHNENTAFEL;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * D'Aboville: empieza por "1" y sigue con UNO o más grupos ".k", donde k es un entero positivo sin ceros
     * a la izquierda, de 1 a MAX_ORDEN. La posición completa no puede pasar de MAX_LONGITUD caracteres.
     */
    public static boolean esDAboville(String posicion) {
        if (posicion == null || posicion.length() > MAX_LONGITUD) return false;
        if (!posicion.startsWith("1.")) return false;

        String[] partes = posicion.split("\\.", -1);
        // Debe tener al menos dos partes (el "1" inicial y al menos un nivel descendiente)
        if (partes.length < 2) return false;
        if (!"1".equals(partes[0])) return false;

        for (int i = 1; i < partes.length; i++) {
            String parte = partes[i];
            if (!parte.matches("^[1-9]\\d*$")) return false;
            try {
                int orden = Integer.parseInt(parte);
                if (orden < 1 || orden > MAX_ORDEN) return false;
            } catch (NumberFormatException e) {
                return false;
            }
        }
        return true;
    }

    /**
     * @return true si es Ahnentafel o d'Aboville (ver esAhnentafel y esDAboville)
     */
    public static boolean esValida(String posicion) {
        return esAhnentafel(posicion) || esDAboville(posicion);
    }

    /**
     * @return true si es un ASCENDIENTE: Ahnentafel válida distinta de "1"
     */
    public static boolean esAscendiente(String posicion) {
        return esAhnentafel(posicion) && !esSujeto(posicion);
    }

    /**
     * @return true si es un DESCENDIENTE: d'Aboville válida (el sujeto "1" NO es descendiente de sí mismo)
     */
    public static boolean esDescendiente(String posicion) {
        return esDAboville(posicion);
    }

    /**
     * Posición del PADRE de la persona Ahnentafel n: 2n. Ej.: "1" -> "2", "5" -> "10".
     */
    public static String posicionDelPadre(String ahnentafel) {
        if (!esAhnentafel(ahnentafel)) {
            throw new IllegalArgumentException("Posición no es Ahnentafel válida: " + ahnentafel);
        }
        int num = Integer.parseInt(ahnentafel);
        return String.valueOf(num * 2);
    }

    /**
     * Posición de la MADRE de la persona Ahnentafel n: 2n + 1. Ej.: "1" -> "3", "5" -> "11".
     */
    public static String posicionDeLaMadre(String ahnentafel) {
        if (!esAhnentafel(ahnentafel)) {
            throw new IllegalArgumentException("Posición no es Ahnentafel válida: " + ahnentafel);
        }
        int num = Integer.parseInt(ahnentafel);
        return String.valueOf(num * 2 + 1);
    }

    /**
     * Posición del HIJO/A (el que está en la línea hacia el sujeto) de un ascendiente: n / 2.
     */
    public static String posicionDelHijoEnLinea(String ahnentafel) {
        if (!esAscendiente(ahnentafel)) {
            throw new IllegalArgumentException("Posición no es un ascendiente válido: " + ahnentafel);
        }
        int num = Integer.parseInt(ahnentafel);
        return String.valueOf(num / 2);
    }

    /**
     * Posición del PROGENITOR de un descendiente: la misma posición sin el último número.
     */
    public static String posicionDelProgenitor(String daboville) {
        if (!esDAboville(daboville)) {
            throw new IllegalArgumentException("Posición no es d'Aboville válida: " + daboville);
        }
        int ultimoPunto = daboville.lastIndexOf('.');
        return daboville.substring(0, ultimoPunto);
    }

    /**
     * Número de orden de un descendiente entre sus hermanos: el último número. Ej.: "1.2.3" -> 3.
     */
    public static int orden(String daboville) {
        if (!esDAboville(daboville)) {
            throw new IllegalArgumentException("Posición no es d'Aboville válida: " + daboville);
        }
        int ultimoPunto = daboville.lastIndexOf('.');
        return Integer.parseInt(daboville.substring(ultimoPunto + 1));
    }

    /**
     * Generaciones de distancia al sujeto.
     */
    public static int generacion(String posicion) {
        if (!esValida(posicion)) {
            throw new IllegalArgumentException("Posición inválida: " + posicion);
        }
        if (esAhnentafel(posicion)) {
            int num = Integer.parseInt(posicion);
            // Parte entera del logaritmo en base 2: Integer.SIZE - 1 - Integer.numberOfLeadingZeros(num)
            return 31 - Integer.numberOfLeadingZeros(num);
        } else {
            // Contar la cantidad de puntos en d'Aboville
            int puntos = 0;
            for (char c : posicion.toCharArray()) {
                if (c == '.') puntos++;
            }
            return puntos;
        }
    }
}