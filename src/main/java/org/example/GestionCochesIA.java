package org.example;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Gestor de una base de datos de coches almacenada en registros de tamaño fijo. */
public class GestionCochesIA {
    private static final int BYTES_MATRICULA = 7;
    private static final int BYTES_MARCA = 32;
    private static final int BYTES_MODELO = 32;
    private static final int TAMANO_REGISTRO = BYTES_MATRICULA + BYTES_MARCA + BYTES_MODELO;
    private static final String ARCHIVO_BD = "coches.dat";

    /** Inicia el menú de texto y atiende opciones hasta que el usuario sale. */
    public static void main(String[] args) {
        boolean salir = false;
        while (!salir) {
            System.out.println("\n--- GESTIÓN DE COCHES ---");
            System.out.println("1. Cargar CSV");
            System.out.println("2. Insertar");
            System.out.println("3. Ordenar por matrícula");
            System.out.println("4. Borrar");
            System.out.println("5. Modificar");
            System.out.println("6. Listar");
            System.out.println("7. Salir");
            int opcion = MiEntradaSalida.leerEntero("Opción: ");
            try {
                switch (opcion) {
                    case 1 -> {
                        String ruta = MiEntradaSalida.leerLinea("Ruta del CSV [src/BBDD Coches.csv]: ").trim();
                        cargarCsv(ruta.isEmpty() ? "src/BBDD Coches.csv" : ruta);
                    }
                    case 2 -> {
                        String matricula = MiEntradaSalida.leerLinea("Matrícula: ").trim();
                        String marca = MiEntradaSalida.leerLinea("Marca: ").trim();
                        String modelo = MiEntradaSalida.leerLinea("Modelo: ").trim();
                        int posicion = MiEntradaSalida.leerEntero("Posición de inserción: ");
                        insertar(matricula, marca, modelo, posicion);
                    }
                    case 3 -> ordenar();
                    case 4 -> {
                        System.out.println("1. Por matrícula  2. Por posición");
                        int modo = MiEntradaSalida.leerEntero("Método: ");
                        if (modo == 1) {
                            borrarPorMatricula(MiEntradaSalida.leerLinea("Matrícula: ").trim());
                        } else if (modo == 2) {
                            borrarPorPosicion(MiEntradaSalida.leerEntero("Posición: "));
                        } else System.out.println("Método no válido.");
                    }
                    case 5 -> {
                        int posicion = MiEntradaSalida.leerEntero("Posición del registro: ");
                        String marca = MiEntradaSalida.leerLinea("Nueva marca: ").trim();
                        String modelo = MiEntradaSalida.leerLinea("Nuevo modelo: ").trim();
                        modificar(posicion, marca, modelo);
                    }
                    case 6 -> listar();
                    case 7 -> salir = true;
                    default -> System.out.println("Opción no válida.");
                }
            } catch (IOException | IllegalArgumentException e) {
                System.out.println("No se pudo completar la operación: " + e.getMessage());
            }
        }
    }
    /** Carga filas de un CSV separado por comas o punto y coma en la base de datos. */
    private static void cargarCsv(String ruta) throws IOException {
        int cargados = 0, omitidos = 0;
        try (BufferedReader reader = Files.newBufferedReader(new File(ruta).toPath(), StandardCharsets.UTF_8);
             RandomAccessFile raf = new RandomAccessFile(ARCHIVO_BD, "rw")) {
            String linea;
            boolean primera = true;
            while ((linea = reader.readLine()) != null) {
                if (linea.isBlank()) continue;
                String[] campos = parsearCsv(linea);
                if (primera && campos.length > 0 && campos[0].replace("\uFEFF", "").trim().equalsIgnoreCase("matrícula")) {
                    primera = false;
                    continue;
                }
                primera = false;
                if (campos.length != 3) { omitidos++; System.out.println("Fila omitida (se esperan 3 campos): " + linea); continue; }
                String matricula = campos[0].trim().toUpperCase();
                validarCampo(matricula, BYTES_MATRICULA, "Matrícula");
                validarCampo(campos[1].trim(), BYTES_MARCA, "Marca");
                validarCampo(campos[2].trim(), BYTES_MODELO, "Modelo");
                if (existeMatricula(raf, matricula)) { omitidos++; System.out.println("Matrícula duplicada, omitida: " + matricula); continue; }
                raf.seek(raf.length());
                escribirRegistro(raf, matricula, campos[1].trim(), campos[2].trim());
                cargados++;
            }
        }
        System.out.println("Carga finalizada: " + cargados + " añadidos, " + omitidos + " omitidos.");
    }

    /** Divide una línea CSV respetando campos entrecomillados y comas escapadas. */
    private static String[] parsearCsv(String linea) {
        List<String> campos = new ArrayList<>();
        StringBuilder campo = new StringBuilder();
        boolean entreComillas = false;
        char separador = linea.indexOf(';') >= 0 && linea.indexOf(',') < 0 ? ';' : ',';
        for (int i = 0; i < linea.length(); i++) {
            char c = linea.charAt(i);
            if (c == '"') {
                if (entreComillas && i + 1 < linea.length() && linea.charAt(i + 1) == '"') { campo.append('"'); i++; }
                else entreComillas = !entreComillas;
            } else if (c == separador && !entreComillas) { campos.add(campo.toString()); campo.setLength(0); }
            else campo.append(c);
        }
        campos.add(campo.toString());
        return campos.toArray(new String[0]);
    }

    /** Inserta un coche en el índice solicitado y desplaza los registros siguientes. */
    private static void insertar(String matricula, String marca, String modelo, int posicion) throws IOException {
        matricula = matricula.toUpperCase();
        validarRegistro(matricula, marca, modelo);
        try (RandomAccessFile raf = new RandomAccessFile(ARCHIVO_BD, "rw")) {
            if (existeMatricula(raf, matricula)) throw new IllegalArgumentException("esa matrícula ya existe.");
            int total = totalRegistros(raf);
            validarPosicion(posicion, total, true);
            raf.setLength((long) (total + 1) * TAMANO_REGISTRO);
            // El desplazamiento inverso evita sobrescribir registros que aún no se han movido.
            for (int i = total; i > posicion; i--) copiarRegistro(raf, i - 1, i);
            raf.seek((long) posicion * TAMANO_REGISTRO);
            escribirRegistro(raf, matricula, marca, modelo);
        }
    }

    /** Ordena los registros por matrícula de forma ascendente. */
    private static void ordenar() throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(ARCHIVO_BD, "rw")) {
            int total = totalRegistros(raf);
            List<byte[]> registros = new ArrayList<>();
            byte[] registro = new byte[TAMANO_REGISTRO];
            for (int i = 0; i < total; i++) { raf.seek((long) i * TAMANO_REGISTRO); raf.readFully(registro); registros.add(registro.clone()); }
            registros.sort((a, b) -> decodificar(a, 0, BYTES_MATRICULA).compareToIgnoreCase(decodificar(b, 0, BYTES_MATRICULA)));
            raf.seek(0);
            for (byte[] bytes : registros) raf.write(bytes);
        }
        System.out.println("Fichero ordenado por matrícula.");
    }

    /** Borra un registro buscándolo por matrícula o por índice. */
    private static void borrarPorMatricula(String matricula) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(ARCHIVO_BD, "rw")) {
            int total = totalRegistros(raf);
            int posicion = buscarMatricula(raf, matricula.toUpperCase());
            if (posicion < 0) throw new IllegalArgumentException("no se encontró esa matrícula.");
            for (int i = posicion; i < total - 1; i++) copiarRegistro(raf, i + 1, i);
            raf.setLength((long) (total - 1) * TAMANO_REGISTRO);
            System.out.println("Registro borrado.");
        }
    }

    /** Borra el registro situado en el índice indicado. */
    private static void borrarPorPosicion(int posicion) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(ARCHIVO_BD, "rw")) {
            int total = totalRegistros(raf);
            validarPosicion(posicion, total, false);
            for (int i = posicion; i < total - 1; i++) copiarRegistro(raf, i + 1, i);
            raf.setLength((long) (total - 1) * TAMANO_REGISTRO);
            System.out.println("Registro borrado.");
        }
    }

    /** Modifica marca y modelo de un registro sin permitir cambiar su matrícula. */
    private static void modificar(int posicion, String marca, String modelo) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(ARCHIVO_BD, "rw")) {
            int total = totalRegistros(raf);
            validarPosicion(posicion, total, false);
            raf.seek((long) posicion * TAMANO_REGISTRO);
            String matricula = leerCampo(raf, BYTES_MATRICULA);
            System.out.println("Matrícula (no modificable): " + matricula);
            validarCampo(marca, BYTES_MARCA, "Marca"); validarCampo(modelo, BYTES_MODELO, "Modelo");
            raf.seek((long) posicion * TAMANO_REGISTRO);
            escribirRegistro(raf, matricula, marca, modelo);
            System.out.println("Registro modificado.");
        }
    }

    /** Imprime todos los registros con sus posiciones. */
    private static void listar() throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(ARCHIVO_BD, "r")) {
            int total = totalRegistros(raf);
            for (int i = 0; i < total; i++) {
                raf.seek((long) i * TAMANO_REGISTRO);
                System.out.printf("%d: %s | %s | %s%n", i, leerCampo(raf, BYTES_MATRICULA), leerCampo(raf, BYTES_MARCA), leerCampo(raf, BYTES_MODELO));
            }
            System.out.println("Total: " + total);
        }
    }

    /** Escribe un registro completo en el punto actual del fichero. */
    private static void escribirRegistro(RandomAccessFile raf, String matricula, String marca, String modelo) throws IOException {
        escribirCampo(raf, matricula, BYTES_MATRICULA); escribirCampo(raf, marca, BYTES_MARCA); escribirCampo(raf, modelo, BYTES_MODELO);
    }

    /** Escribe texto UTF-8 rellenando hasta el ancho fijo, sin cortar caracteres multibyte. */
    private static void escribirCampo(RandomAccessFile raf, String texto, int ancho) throws IOException {
        byte[] origen = texto.getBytes(StandardCharsets.UTF_8);
        if (origen.length > ancho) throw new IllegalArgumentException("el campo excede " + ancho + " bytes.");
        byte[] salida = new byte[ancho]; Arrays.fill(salida, (byte) ' '); System.arraycopy(origen, 0, salida, 0, origen.length); raf.write(salida);
    }

    /** Lee un campo fijo desde la posición actual del fichero. */
    private static String leerCampo(RandomAccessFile raf, int ancho) throws IOException {
        byte[] bytes = new byte[ancho]; raf.readFully(bytes); return new String(bytes, StandardCharsets.UTF_8).trim();
    }

    /** Comprueba si la matrícula ya está almacenada. */
    private static boolean existeMatricula(RandomAccessFile raf, String matricula) throws IOException { return buscarMatricula(raf, matricula) >= 0; }

    /** Devuelve el índice de una matrícula o -1 si no existe. */
    private static int buscarMatricula(RandomAccessFile raf, String matricula) throws IOException {
        int total = totalRegistros(raf);
        for (int i = 0; i < total; i++) { raf.seek((long) i * TAMANO_REGISTRO); if (leerCampo(raf, BYTES_MATRICULA).equalsIgnoreCase(matricula)) return i; }
        return -1;
    }

    /** Copia un registro completo entre dos posiciones. */
    private static void copiarRegistro(RandomAccessFile raf, int origen, int destino) throws IOException {
        byte[] bytes = new byte[TAMANO_REGISTRO]; raf.seek((long) origen * TAMANO_REGISTRO); raf.readFully(bytes); raf.seek((long) destino * TAMANO_REGISTRO); raf.write(bytes);
    }

    /** Devuelve el número de registros y rechaza ficheros con longitud inválida. */
    private static int totalRegistros(RandomAccessFile raf) throws IOException {
        if (raf.length() % TAMANO_REGISTRO != 0) throw new IOException("el fichero contiene un registro incompleto.");
        long cantidad = raf.length() / TAMANO_REGISTRO;
        if (cantidad > Integer.MAX_VALUE) throw new IOException("el fichero contiene demasiados registros.");
        return (int) cantidad;
    }

    /** Válida los tres campos de un coche antes de guardarlo. */
    private static void validarRegistro(String matricula, String marca, String modelo) {
        validarCampo(matricula, BYTES_MATRICULA, "Matrícula"); validarCampo(marca, BYTES_MARCA, "Marca"); validarCampo(modelo, BYTES_MODELO, "Modelo");
    }

    /** Válida que un texto no esté vacío y que quepa en su campo en UTF-8. */
    private static void validarCampo(String texto, int ancho, String nombre) {
        if (texto == null || texto.isBlank()) throw new IllegalArgumentException(nombre + " no puede estar vacío.");
        if (texto.getBytes(StandardCharsets.UTF_8).length > ancho) throw new IllegalArgumentException(nombre + " supera " + ancho + " bytes UTF-8.");
    }

    /** Comprueba si una posición es válida para inserción o acceso existente. */
    private static void validarPosicion(int posicion, int total, boolean permiteFinal) {
        int limite = permiteFinal ? total : total - 1;
        if (posicion < 0 || posicion > limite) throw new IllegalArgumentException("posición fuera de rango.");
    }

    /** Decodifica un campo desde un registro binario completo. */
    private static String decodificar(byte[] registro, int inicio, int ancho) {
        return new String(registro, inicio, ancho, StandardCharsets.UTF_8).trim();
    }
}
