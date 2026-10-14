package org.example;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/** Gestiona coches en un fichero binario de registros de longitud fija. */
public class GestionCoches {

    // La matrícula ocupa exactamente los 7 bytes definidos por el formato del ejercicio.
    private static final int BYTES_MATRICULA = 7;
    // La marca reserva 32 bytes aunque el texto real sea más corto.
    private static final int BYTES_MARCA = 32;
    // El modelo también reserva 32 bytes en todos los registros.
    private static final int BYTES_MODELO = 32;
    // Cada registro mide 71 bytes: 7 de matrícula, 32 de marca y 32 de modelo.
    private static final int TAMANO_REGISTRO = BYTES_MATRICULA + BYTES_MARCA + BYTES_MODELO;
    // Esta colección auxiliar conserva bloques binarios completos, sin crear una clase Coche.
    Set<byte[]> listaDeRegistros = new HashSet<>();
    // Los campos cortos se completan con espacios hasta alcanzar su tamaño fijo.
    private static final byte BYTE_ESPACIO = (byte) ' ';
    // Nombre del fichero binario que actúa como base de datos.
    private static final String ARCHIVO_BD = "coches.dat";
    // Referencia al mismo fichero para las operaciones que consultan sus propiedades.
    File archivo = new File(ARCHIVO_BD);


    /** Presenta el menú y ejecuta las acciones hasta que el usuario decide salir. */
    public static void main(String[] args) {
        // Mantiene el menú activo hasta que se seleccione la opción de salida.
        boolean salir = false;

        // Repite la presentación del menú después de cada operación.
        while (!salir) {
            // Muestra las operaciones disponibles para el usuario.
            System.out.println("\n--- GESTIÓN DE COCHES ---");
            System.out.println("1. Cargar información de CSV");
            System.out.println("2. Insertar coche en posición concreta");
            System.out.println("3. Ordenar fichero por matrícula");
            System.out.println("4. Borrar un registro");
            System.out.println("5. Modificar un registro");
            System.out.println("6. Mostrar todos los coches (Extra, para comprobar)");
            System.out.println("7. Salir");

            // Agrupa los errores de entrada y de fichero para que el menú pueda continuar.
            try {
                // MiEntradaSalida vuelve a pedir el dato si no se introduce un entero.
                int opcion = MiEntradaSalida.leerEntero("Elige una opción: ");

                // Ejecuta la operación asociada con la opción elegida.
                switch (opcion) {
                    case 1:
                        // Permite usar la ruta habitual pulsando Intro.
                        String ruta = MiEntradaSalida.leerLinea("Ruta del CSV [src/BBDD Coches.csv]: ").trim();
                        // Si no se escribió una ruta alternativa, carga el CSV del proyecto.
                        cargarCSV(ruta.isEmpty() ? "src/BBDD Coches.csv" : ruta);
                        break;
                    case 2:
                        // Lee los datos del coche y su índice de inserción, comenzando en cero.
                        insertar(MiEntradaSalida.leerLinea("Matrícula: ").trim(), MiEntradaSalida.leerLinea("Marca: ").trim(), MiEntradaSalida.leerLinea("Modelo: ").trim(), MiEntradaSalida.leerEntero("Posición (desde 0): "));
                        break;
                    case 3:
                        // Ordena todos los registros usando la matrícula como criterio.
                        ordenar();
                        break;
                    case 4:
                        // El usuario elige si localizar el registro por clave o por índice.
                        int modo = MiEntradaSalida.leerEntero("1. Matrícula, 2. Posición: ");
                        // La opción 1 busca por la clave primaria matrícula.
                        if (modo == 1) borrarPorMatricula(MiEntradaSalida.leerLinea("Matrícula: ").trim());
                        // La opción 2 borra directamente por su índice físico.
                        else if (modo == 2) borrarPorPosicion(MiEntradaSalida.leerEntero("Posición (desde 0): "));
                        // Cualquier otro valor no coincide con una operación de borrado.
                        else System.out.println("Opción no válida.");
                        break;
                    case 5:
                        // Se recogen únicamente marca y modelo para no cambiar la matrícula.
                        int posicion = MiEntradaSalida.leerEntero("Posición (desde 0): ");
                        modificar(posicion, MiEntradaSalida.leerLinea("Nueva marca: ").trim(), MiEntradaSalida.leerLinea("Nuevo modelo: ").trim());
                        break;
                    case 6:
                        // Facilita comprobar el contenido y los índices actuales del fichero.
                        listarCoches();
                        break;
                    case 7:
                        // Cambia la condición del bucle para cerrar el menú.
                        salir = true;
                        System.out.println("Saliendo...");
                        break;
                    default:
                        // Informa cuando el usuario introduce una opción fuera del menú.
                        System.out.println("Opción incorrecta.");
                }
            } catch (NumberFormatException e) {
                // Informa del error de formato numérico sin terminar el programa.
                System.out.println("Error: Introduce un número válido.");
            } catch (IOException | IllegalArgumentException e) {
                // Informa del problema de fichero o de validación y permite otra operación.
                // Se informa del error de fichero o validación y se mantiene activo el menú.
                System.out.println("No se pudo completar la operación: " + e.getMessage());
            }
        }
    }

    /**
     * Paso 1: Carga los datos del archivo CSV al fichero binario.
     */
    private static void cargarCSV(String nombreArchivoCsv) {
        // Crea una referencia al archivo de texto que se va a importar.
        File archivoCsv = new File(nombreArchivoCsv);
        // Evita intentar abrir una ruta que no exista.
        if (!archivoCsv.exists()) {
            System.out.println("Error: El archivo CSV no se encuentra.");
            return;
        }

        // El lector con buffer procesa el CSV línea por línea y RandomAccessFile escribe el binario.
        try (BufferedReader br = new BufferedReader(new FileReader(archivoCsv));
             RandomAccessFile raf = new RandomAccessFile(ARCHIVO_BD, "rw")) {

            // Guarda cada línea leída del CSV.
            String linea;
            // Cuenta los registros nuevos que se incorporen al fichero.
            int contador = 0;
            // La primera fila es la cabecera de nombres de columna y no es un coche.
            br.readLine();
            // Leemos el CSV texto a texto
            while ((linea = br.readLine()) != null) {
                // Acepta CSV separados por coma o por punto y coma.
                String[] partes = linea.split("[,;]");
                // Solo se procesa la fila si contiene matrícula, marca y modelo.
                if (partes.length >= 3) {
                    // Quita los espacios que rodean a cada valor del CSV.
                    String matricula = partes[0].trim();
                    String marca = partes[1].trim();
                    String modelo = partes[2].trim();

                    // Evitamos truncar datos que no quepan en los campos fijos del fichero.
                    validarRegistro(matricula, marca, modelo);

                    // Usamos al ayudante para ver si ya existe
                    if (!existeMatricula(raf, matricula)) {

                        // Si no existe, vamos al final del archivo para añadir el nuevo coche
                        raf.seek(raf.length());

                        // Usamos la herramienta segura para guardar los bytes exactos
                        escribirCampoFijo(raf, matricula, BYTES_MATRICULA);
                        escribirCampoFijo(raf, marca, BYTES_MARCA);
                        escribirCampoFijo(raf, modelo, BYTES_MODELO);

                        // Actualiza el total de coches añadidos correctamente.
                        contador++;
                    } else {
                        // Omite claves duplicadas para mantener única la matrícula.
                        System.out.println("Aviso: La matrícula " + matricula + " ya existe. Se omite.");
                    }
                }
            }
            // Resume cuántos registros se añadieron al fichero.
            System.out.println("Carga completada. Se han insertado " + contador + " coches.");

        } catch (IOException e) {
            // Captura fallos al leer el CSV o escribir la base de datos.
            System.out.println("Error de entrada/salida: " + e.getMessage());
        }
    }

    /** Convierte un campo a su representación de bytes de longitud fija. */
    private byte[] formatearABytesFijos(String nombreCampo, String texto, int longitudFija) {
        // Codifica el texto en UTF-8 antes de medirlo y guardarlo.
        byte[] bytesOriginales = texto.getBytes(StandardCharsets.UTF_8);

        // Rechaza valores que no caben para evitar alterar el tamaño del registro.
        if (bytesOriginales.length > longitudFija) {
            throw new IllegalArgumentException(
                    String.format("El campo '%s' ocupa %d bytes (máximo permitido: %d bytes).",
                            nombreCampo, bytesOriginales.length, longitudFija)
            );
        }

        // Reserva un arreglo del tamaño requerido por el campo.
        byte[] bytesResultantes = new byte[longitudFija];
        // Inicializa todos los bytes con espacios de relleno.
        Arrays.fill(bytesResultantes, BYTE_ESPACIO);
        // Copia el texto al comienzo y deja el relleno al final.
        System.arraycopy(bytesOriginales, 0, bytesResultantes, 0, bytesOriginales.length);

        // Devuelve los bytes listos para escribirse en el fichero.
        return bytesResultantes;
    }

    /** Recupera secuencialmente los registros del fichero en la colección auxiliar. */
    private void actualizarVariable() throws AppException{
        // Abre la base de datos en modo lectura y cierra el flujo al salir del bloque.
        try (RandomAccessFile arc = new RandomAccessFile(archivo, "r")){
           // Continúa mientras quede espacio para leer un registro completo.
           while (arc.getFilePointer() + TAMANO_REGISTRO <= arc.length()){
               // Reserva memoria para un registro binario completo.
               byte[] buffer = new byte[TAMANO_REGISTRO];
               // Lee exactamente los bytes del registro actual.
               arc.readFully(buffer);

               // Guarda el bloque leído en la colección auxiliar.
               listaDeRegistros.add(buffer);
           }
        } catch (IOException e) {
            // Convierte el error de entrada/salida al tipo de excepción de la aplicación.
            throw new AppException("No se pudo volcar la información de la base de datos");
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

    /** Busca secuencialmente una matrícula para impedir claves duplicadas. */
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
    /**
     * Copia un coche entero (71 bytes) de una posición a otra.
     */
    private static void copiarRegistro(RandomAccessFile raf, long posOrigen, long posDestino) throws IOException {
        byte[] buffer = new byte[TAMANO_REGISTRO]; // Caja para los 71 bytes

        // 1. Leemos el coche de origen
        raf.seek(posOrigen * TAMANO_REGISTRO);
        raf.readFully(buffer);

        // 2. Lo pegamos en el destino
        raf.seek(posDestino * TAMANO_REGISTRO);
        raf.write(buffer);
    }

    /** Inserta un coche en una posición y desplaza hacia atrás los registros posteriores. */
    private static void insertar(String matricula, String marca, String modelo, int posicion) throws IOException {
        matricula = matricula.trim().toUpperCase();
        validarRegistro(matricula, marca, modelo);
        try (RandomAccessFile raf = new RandomAccessFile(ARCHIVO_BD, "rw")) {
            int total = totalRegistros(raf);
            validarPosicion(posicion, total, true);
            if (existeMatricula(raf, matricula)) throw new IllegalArgumentException("La matrícula ya existe.");
            raf.setLength((long) (total + 1) * TAMANO_REGISTRO);
            // Se copia desde el final para no sobrescribir ningún registro aún pendiente de mover.
            for (int i = total; i > posicion; i--) copiarRegistro(raf, i - 1, i);
            raf.seek((long) posicion * TAMANO_REGISTRO);
            escribirRegistro(raf, matricula, marca, modelo);
        }
        System.out.println("Coche insertado.");
    }

    /** Ordena ascendentemente el fichero por matrícula. */
    private static void ordenar() throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(ARCHIVO_BD, "rw")) {
            int total = totalRegistros(raf);
            byte[][] registros = new byte[total][TAMANO_REGISTRO];
            for (int i = 0; i < total; i++) { raf.seek((long) i * TAMANO_REGISTRO); raf.readFully(registros[i]); }
            Arrays.sort(registros, (a, b) -> matriculaDe(a).compareToIgnoreCase(matriculaDe(b)));
            raf.seek(0);
            for (byte[] registro : registros) raf.write(registro);
        }
        System.out.println("Fichero ordenado por matrícula.");
    }

    /** Extrae la matrícula del bloque binario de un registro. */
    private static String matriculaDe(byte[] registro) {
        return new String(registro, 0, BYTES_MATRICULA, StandardCharsets.UTF_8).trim();
    }

    /** Borra un registro buscando su matrícula. */
    private static void borrarPorMatricula(String matricula) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(ARCHIVO_BD, "rw")) {
            int posicion = buscarMatricula(raf, matricula);
            if (posicion < 0) throw new IllegalArgumentException("No se encontró esa matrícula.");
            borrarEnPosicion(raf, posicion);
        }
        System.out.println("Registro borrado.");
    }

    /** Borra un registro indicando su posición desde cero. */
    private static void borrarPorPosicion(int posicion) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(ARCHIVO_BD, "rw")) {
            validarPosicion(posicion, totalRegistros(raf), false);
            borrarEnPosicion(raf, posicion);
        }
        System.out.println("Registro borrado.");
    }

    /** Elimina físicamente un registro, compactando los posteriores. */
    private static void borrarEnPosicion(RandomAccessFile raf, int posicion) throws IOException {
        int total = totalRegistros(raf);
        for (int i = posicion; i < total - 1; i++) copiarRegistro(raf, i + 1, i);
        raf.setLength((long) (total - 1) * TAMANO_REGISTRO);
    }

    /** Modifica marca y modelo de un registro y deja intacta su matrícula. */
    private static void modificar(int posicion, String marca, String modelo) throws IOException {
        validarCampo(marca, BYTES_MARCA, "Marca");
        validarCampo(modelo, BYTES_MODELO, "Modelo");
        try (RandomAccessFile raf = new RandomAccessFile(ARCHIVO_BD, "rw")) {
            validarPosicion(posicion, totalRegistros(raf), false);
            raf.seek((long) posicion * TAMANO_REGISTRO);
            String matricula = leerCampoFijo(raf, BYTES_MATRICULA);
            raf.seek((long) posicion * TAMANO_REGISTRO);
            escribirRegistro(raf, matricula, marca, modelo);
        }
        System.out.println("Registro modificado; matrícula conservada.");
    }

    /** Muestra todos los registros con su índice. */
    private static void listarCoches() throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(ARCHIVO_BD, "r")) {
            int total = totalRegistros(raf);
            for (int i = 0; i < total; i++) {
                raf.seek((long) i * TAMANO_REGISTRO);
                System.out.printf("%d: %s | %s | %s%n", i, leerCampoFijo(raf, BYTES_MATRICULA), leerCampoFijo(raf, BYTES_MARCA), leerCampoFijo(raf, BYTES_MODELO));
            }
            System.out.println("Total: " + total);
        }
    }

    /** Escribe los campos de un registro en el orden fijado por el formato. */
    private static void escribirRegistro(RandomAccessFile raf, String matricula, String marca, String modelo) throws IOException {
        escribirCampoFijo(raf, matricula, BYTES_MATRICULA);
        escribirCampoFijo(raf, marca, BYTES_MARCA);
        escribirCampoFijo(raf, modelo, BYTES_MODELO);
    }

    /** Devuelve el número de registros y detecta un último registro incompleto. */
    private static int totalRegistros(RandomAccessFile raf) throws IOException {
        if (raf.length() % TAMANO_REGISTRO != 0) throw new IOException("El fichero contiene un registro incompleto.");
        long cantidad = raf.length() / TAMANO_REGISTRO;
        if (cantidad > Integer.MAX_VALUE) throw new IOException("El fichero es demasiado grande.");
        return (int) cantidad;
    }

    /** Busca una matrícula de forma secuencial y devuelve su posición o -1. */
    private static int buscarMatricula(RandomAccessFile raf, String matricula) throws IOException {
        int total = totalRegistros(raf);
        for (int i = 0; i < total; i++) {
            raf.seek((long) i * TAMANO_REGISTRO);
            if (leerCampoFijo(raf, BYTES_MATRICULA).equalsIgnoreCase(matricula)) return i;
        }
        return -1;
    }

    /** Comprueba que un campo no esté vacío y que quepa en bytes UTF-8. */
    private static void validarCampo(String texto, int ancho, String nombre) {
        if (texto == null || texto.isBlank()) throw new IllegalArgumentException(nombre + " no puede estar vacío.");
        if (texto.getBytes(StandardCharsets.UTF_8).length > ancho) throw new IllegalArgumentException(nombre + " supera " + ancho + " bytes.");
    }

    /** Valida los tres campos del coche antes de escribirlo. */
    private static void validarRegistro(String matricula, String marca, String modelo) {
        validarCampo(matricula, BYTES_MATRICULA, "Matrícula");
        validarCampo(marca, BYTES_MARCA, "Marca");
        validarCampo(modelo, BYTES_MODELO, "Modelo");
    }

    /** Valida una posición de inserción o de un registro existente. */
    private static void validarPosicion(int posicion, int total, boolean permiteFinal) {
        int limite = permiteFinal ? total : total - 1;
        if (posicion < 0 || posicion > limite) throw new IllegalArgumentException("Posición fuera de rango.");
    }
}
