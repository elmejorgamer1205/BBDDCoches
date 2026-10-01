package org.example;

import java.io.*;
import java.util.Scanner;

public class GestionCoches {

    private static final int BYTES_MATRICULA = 7;
    private static final int BYTES_MARCA = 32;
    private static final int BYTES_MODELO = 32;
    private static final int TAMANO_REGISTRO = BYTES_MATRICULA + BYTES_MARCA + BYTES_MODELO;

    private static final String ARCHIVO_BD = "coches.dat";

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        boolean salir = false;

        while (!salir) {
            System.out.println("\n--- GESTIÓN DE COCHES ---");
            System.out.println("1. Cargar información de CSV");
            System.out.println("2. Insertar coche en posición concreta");
            System.out.println("3. Ordenar fichero por matrícula");
            System.out.println("4. Borrar un registro");
            System.out.println("5. Modificar un registro");
            System.out.println("6. Mostrar todos los coches (Extra, para comprobar)");
            System.out.println("7. Salir");
            System.out.print("Elige una opción: ");

            try {
                int opcion = Integer.parseInt(scanner.nextLine());

                switch (opcion) {
                    case 1:
                        cargarCSV("BBDD Coches.csv");
                        break;
                    case 2:
                        System.out.println("Pendiente: Insertar coche...");
                        break;
                    case 3:
                        System.out.println("Pendiente: Ordenar...");
                        break;
                    case 4:
                        System.out.println("Pendiente: Borrar...");
                        break;
                    case 5:
                        System.out.println("Pendiente: Modificar...");
                        break;
                    case 6:
                        listarCoches();
                        break;
                    case 7:
                        salir = true;
                        System.out.println("Saliendo...");
                        break;
                    default:
                        System.out.println("Opción incorrecta.");
                }
            } catch (NumberFormatException e) {
                System.out.println("Error: Introduce un número válido.");
            }
        }
    }

    /**
     * Escribe un texto en el fichero ocupando exactamente la longitud indicada en bytes.
     * Adaptado del código del maestro para soportar tildes y caracteres especiales.
     */
    private static void escribirCampoFijo(RandomAccessFile raf, String texto, int longitudFija) throws IOException {
        // 1. Convertimos el texto a bytes reales usando UTF-8
        byte[] bytesOriginales = texto.getBytes(java.nio.charset.StandardCharsets.UTF_8);

        // 2. Preparamos un array del tamaño exacto que necesitamos y lo rellenamos de espacios
        byte[] bytesResultantes = new byte[longitudFija];
        java.util.Arrays.fill(bytesResultantes, (byte) ' ');

        // 3. Copiamos los bytes del texto al array resultante
        int longitudACopiar = Math.min(bytesOriginales.length, longitudFija);
        System.arraycopy(bytesOriginales, 0, bytesResultantes, 0, longitudACopiar);

        // 4. Escribimos el bloque exacto en el fichero
        raf.write(bytesResultantes);
    }

    /**
     * Lee un número exacto de bytes del fichero y lo convierte a String.
     */
    private static String leerCampoFijo(RandomAccessFile raf, int longitudFija) throws IOException {
        byte[] buffer = new byte[longitudFija];
        raf.readFully(buffer);

        // Convertimos los bytes leídos de vuelta a texto usando UTF-8 y quitamos los espacios de relleno
        return new String(buffer, java.nio.charset.StandardCharsets.UTF_8).trim();
    }

    /**
     * Comprueba si una matrícula ya existe en nuestro fichero binario (coches.dat).
     */
    private static boolean existeMatricula(RandomAccessFile raf, String matriculaBuscada) throws IOException {
        long totalRegistros = raf.length() / TAMANO_REGISTRO;

        for (long i = 0; i < totalRegistros; i++) {
            raf.seek(i * TAMANO_REGISTRO);
            String matriculaLeida = leerCampoFijo(raf, BYTES_MATRICULA);

            if (matriculaLeida.equalsIgnoreCase(matriculaBuscada)) {
                return true; // ¡La encontró!
            }
        }
        return false; // No la encontró
    }

    /**
     * Paso 1: Carga los datos del archivo CSV al fichero binario.
     */
    private static void cargarCSV(String nombreArchivoCsv) {
        File archivoCsv = new File(nombreArchivoCsv);
        if (!archivoCsv.exists()) {
            System.out.println("Error: El archivo CSV no se encuentra.");
            return;
        }

        try (BufferedReader br = new BufferedReader(new FileReader(archivoCsv));
             RandomAccessFile raf = new RandomAccessFile(ARCHIVO_BD, "rw")) {

            String linea;
            int contador = 0;

            // Leemos el CSV texto a texto
            while ((linea = br.readLine()) != null) {
                String[] partes = linea.split("[,;]");
                if (partes.length >= 3) {
                    String matricula = partes[0].trim();
                    String marca = partes[1].trim();
                    String modelo = partes[2].trim();

                    // ¡Aquí está el truco! Usamos al ayudante para ver si ya existe
                    if (!existeMatricula(raf, matricula)) {

                        // Si no existe, vamos al final del archivo para añadir el nuevo coche
                        raf.seek(raf.length());

                        // Usamos la herramienta segura para guardar los bytes exactos
                        escribirCampoFijo(raf, matricula, BYTES_MATRICULA);
                        escribirCampoFijo(raf, marca, BYTES_MARCA);
                        escribirCampoFijo(raf, modelo, BYTES_MODELO);

                        contador++;
                    } else {
                        System.out.println("Aviso: La matrícula " + matricula + " ya existe. Se omite.");
                    }
                }
            }
            System.out.println("Carga completada. Se han insertado " + contador + " coches.");

        } catch (IOException e) {
            System.out.println("Error de entrada/salida: " + e.getMessage());
        }
    }
}
