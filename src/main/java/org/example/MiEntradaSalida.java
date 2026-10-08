package org.example;

import aventura.app.exceptions.MiEntrdadaSalidaException;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Scanner;

//Version2.0
public class MiEntradaSalida {
    private static Scanner sc;

    private static Scanner getScanner() {
        if (sc == null) {
            sc = new Scanner(System.in);
        }
        return sc;
    }

    /**
     * Lee un entero mostrando el mensaje pasado como parámetro
     *
     * @param mensaje El mensaje a mostrar
     * @return el entero leído por teclado
     */
    public static int leerEntero(String mensaje) {
        int integer = 0;
        // Variable que almacenará un booleano que indicará si se le debe volver a pedir el dato al usuario.
        boolean flag = true;

        while (flag) {
            // Pedimos el entero por pantalla.
            System.out.println(mensaje);
            // Comprobamos si el usuario está introduciendo algo correcto usando la excepción del método parseInt.
            try {
                integer = Integer.parseInt(getScanner().nextLine());
                // Si llegamos hasta aquí, es porque el usuario ha introducido un dato correcto.
                flag = false;
            }
            // Si se lanza la excepción, informamos al usuario de su error.
            catch (NumberFormatException e) {
                // 2. Mensaje de error específico.
                System.out.println("Error: Debe introducir un número entero.");
            }

        }

        return integer;
    }

    /**
     * Número entero
     *
     * @param mensaje      El mensaje a mostrar
     * @param seAceptaCero Si el 0 esta incluido
     * @return El entero leido por teclado
     */
    public static int leerEnteroPositivo(String mensaje, boolean seAceptaCero) {
        int a;
        do {
            a = leerEntero(mensaje);
            if (a < 0 || a == 0 && !seAceptaCero) {
                System.out.println("Numero no válido.");
            }

        } while (a < 0 || a == 0 && !seAceptaCero);
        return a;
    }

    /**
     * Leer un decimal de tipo double
     *
     * @param mensaje El mensaje a introducir
     * @return Lo introducido por el usuario
     */
    public static double leerDouble(String mensaje) {
        double integer = 0;
        // Variable que almacenará un booleano que indicará si se le debe volver a pedir el dato al usuario.
        boolean flag = true;

        while (flag) {
            // Pedimos el entero por pantalla.
            System.out.println(mensaje);
            // Comprobamos si el usuario está introduciendo algo correcto usando la excepción del método parseInt.
            try {
                integer = Double.parseDouble(getScanner().nextLine());
                // Si llegamos hasta aquí, es porque el usuario ha introducido un dato correcto.
                flag = false;
            }
            // Si se lanza la excepción, informamos al usuario de su error.
            catch (NumberFormatException e) {
                // 2. Mensaje de error específico.
                System.out.println("Error: Debe introducir un número decimal.");
            }

        }

        return integer;
    }

    /**
     * Leer un decimal de tipo Float
     *
     * @param mensaje mensaje a mostrar
     * @return lo introducido por el usuario
     */
    public static float leerFloat(String mensaje) {
        float integer = 0;
        // Variable que almacenará un booleano que indicará si se le debe volver a pedir el dato al usuario.
        boolean flag = true;

        while (flag) {
            // Pedimos el entero por pantalla.
            System.out.println(mensaje);
            // Comprobamos si el usuario está introduciendo algo correcto usando la excepción del método parseInt.
            try {
                integer = Float.parseFloat(getScanner().nextLine());
                // Si llegamos hasta aquí, es porque el usuario ha introducido un dato correcto.
                flag = false;
            }
            // Si se lanza la excepción, informamos al usuario de su error.
            catch (NumberFormatException e) {
                // 2. Mensaje de error específico.
                System.out.println("Error: Debe introducir un número entero.");
            }

        }

        return integer;
    }


    /**
     * Generar un número aleatorio determinado por un máximo
     *
     * @param max hasta que número se va a generar
     * @return número generado aleatoriamente
     */
    public static int generaAleatorio(int max) {
        return (int) (Math.random() * max + 1);
    }

    /**
     * Generar un número aleatorio entre un máximo y un mínimo
     *
     * @param min              Primer número del intervalo
     * @param max              Segundo número del intervalo
     * @param seAceptaElMaximo ¿El máximo entra en ese intervalo?
     * @return El número genrado aleatoriamente
     */
    public static int generaAleatorioEntre(int min, int max, boolean seAceptaElMaximo) {
        int ventana;

        if (seAceptaElMaximo) {
            ventana = max - min + 1;
        } else {
            ventana = max - min;
        }
        return (int) (Math.random() * ventana) + min;
    }

    /**
     * Leer una cadena de texto
     *
     * @param mensaje Mensaje a mostrar
     * @return String leido
     */
    public static String leerTexto(String mensaje) {
        System.out.print(mensaje);
        return getScanner().next();
    }

    /**
     * Leer entero en un rango
     *
     * @param mensaje mensaje a mostrar
     * @param min     valor mínimo incluido
     * @param max     valor máximo incluido
     * @return Numero leido por teclado
     */
    public static int leerEnteroRango(String mensaje, int min, int max) throws MiEntrdadaSalidaException {
        if (min > max) {
            //Mostrar error
            throw new MiEntrdadaSalidaException("El mínimo es mayor que el máximo");
        }
        int a;
        do {
            a = leerEntero(mensaje);
            if (a < min || a > max) {
                System.out.println("Número no válido, esta fuera del rango.");
            }
        } while (a < min || a > max);
        return a;
    }

    /**
     * Recoger el caracter 'S' o 'N'
     *
     * @param mensaje Mensaje a mostrar
     * @return El caracter obtenido por teclado
     */
    public static char leerSN(String mensaje) {
        char caracter;
        do {
            String texto = leerTexto(mensaje).toUpperCase();
            caracter = texto.charAt(0);
            if (caracter != 'S' && caracter != 'N') {
                System.out.println("Respuesta no válida");
            }
        } while (caracter != 'S' && caracter != 'N');
        return caracter;
    }

    /**
     * Imprimir una Matriz por pantalla
     *
     * @param matriz La matriz a imprimir
     */
    public static void imprimirMatriz(int[][] matriz) {
        for (int i = 0; i < matriz.length; i++) {
            for (int j = 0; j < matriz[i].length; j++) {
                System.out.print("[" + matriz[i][j] + "]" + " ");
            }
            System.out.println();
        }
    }

    /**
     * Imprimir una matriz de strings
     *
     * @param matriz matriz a imprimir
     */
    public static void imprimirMatrizString(String[][] matriz) {
        for (int i = 0; i < matriz.length; i++) {
            for (int j = 0; j < matriz[i].length; j++) {
                System.out.print("[" + matriz[i][j] + "]" + " ");
            }
            System.out.println();
        }
    }

    /**
     * Leer linea de texto
     *
     * @param mensaje mensaje a mostar
     * @return mensaje introducido
     */
    public static String leerLinea(String mensaje) {
        System.out.print(mensaje);
        return getScanner().nextLine();
    }

    /**
     * Mostrar opciones con un array
     *
     * @param mensaje mensaje a mostrar
     * @param array   lista a recorrer
     */
    public static void mostrarOpcionesSinNulos(String mensaje, Object[] array) {
        System.out.println(mensaje);
        int posicion = 1;
        for (int i = 0; i < array.length; i++) {
            if (array[i] != null) {
                System.out.println(posicion++ + "." + " " + array[i]);
            }
        }
        System.out.println();
    }

    /**
     * Mostrar opciones con una colección
     *
     * @param mensaje mensaje a mostrar
     * @param coleccion   coleccion a recorrer
     */
    public static void mostrarOpcionesSinNulos(String mensaje, Collection<Object> coleccion) {
        System.out.println(mensaje);
        int posicion = 1;
        for (Object o: coleccion) {
            if (o != null) {
                System.out.println(posicion++ + "." + " " + o);
            }
        }
        System.out.println();
    }

    public static LocalDate fecha(String mensaje) throws MiEntrdadaSalidaException {
        int año =  leerEnteroPositivo("Introduce el año: ",true);
        int mes =  leerEnteroPositivo("Introduce el mes: ",true);
        int dia =  leerEnteroPositivo("Introduce el dia: ",true);
        try {
            return LocalDate.of(año,mes,dia);
        }catch (DateTimeException e){
            throw new MiEntrdadaSalidaException("Fecha no valida.");
        }

    }

    public static char leerChar(String mensaje) throws MiEntrdadaSalidaException{
        String texto = leerTexto(mensaje);
        if (texto.length() != 1) {throw new MiEntrdadaSalidaException("Debes introducir un caracter.");}
        return texto.charAt(0);
    }

}