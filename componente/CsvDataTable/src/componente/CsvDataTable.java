/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

/**
 * Paquete llamado "componente" que contiene el componente visual CsvDataTable.
 * Este componente permite cargar y visualizar archivos CSV en una tabla con paginación, búsqueda y ordenamiento.
 * Además, ofrece personalización visual desde el editor de NetBeans.
 */
package componente;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap; 
import java.util.Map;

/**
 *  Panel de muestra de tabla de archvi CSV (CsvDataTable) es un componente visual personalizado que extiende JPanel y permite:
 * - Cargar datos desde archivos CSV.
 * - Mostrar los datos en una tabla con paginación configurable.
 * - Personalizar el estilo (fuente, colores).
 * - Buscar coincidencias exactas por palabra clave.
 * - Ordenar columnas por clic en el encabezado (3 estados: ascendente, descendente, original).
 * - Restablecer la vista original de los datos cargados.
 *
 * Este componente es ideal para mostrar y explorar grandes conjuntos de datos tabulares en aplicaciones Java Swing.
 *
 * @author Jarquín Rivera Orlando Miguel
 * @author Pérez Ríos Yael Amir
 * Equipo 9 
 */
public class CsvDataTable extends JPanel {

    private JTable table;
    private JScrollPane scrollPane;
    private JLabel lblTitulo;
    private JPanel pnlPaginacion;
    private JButton btnCargarCsv, btnBuscar, btnRestablecer;
    private JTextField txtBuscar;
    private JPanel topPanel;

    // Propiedades configurables
    private Color colorFondo = Color.WHITE;
    private Color colorTexto = Color.BLACK;
    private Font fuenteTexto = new Font("SansSerif", Font.PLAIN, 12);
    private String titulo = "Panel para mostrar tabla en archivo CSV";
    private boolean modoBusqueda = true;
    
    //Configuración de paginación
    private String opcionFilasPorPagina = "20";
    private int filasPorPaginaPersonalizado = 20;
    private int filasPorPagina = 20;
    
    // Datos
    private List<String[]> todosLosDatos = new ArrayList<>();
    private List<String[]> datosOriginales = new ArrayList<>();
    private String[] encabezadosColumna;
    private int paginaActual = 0;
    
    //Ordenamiento
    private Map<Integer, Integer> ordenarEstados = new HashMap<>();

    /**
    * Constructor del componente. Inicializa todos los elementos visuales,
    * configura los eventos de los botones y define el comportamiento de ordenamiento y búsqueda.
    */
    public CsvDataTable() {
        setLayout(new BorderLayout());

        table = new JTable();
        scrollPane = new JScrollPane(table);
        pnlPaginacion = new JPanel(new FlowLayout(FlowLayout.CENTER));
        lblTitulo = new JLabel("", SwingConstants.CENTER);
        btnCargarCsv = new JButton("Cargar CSV");
        btnBuscar = new JButton("Buscar");
        btnRestablecer = new JButton("Restablecer");
        txtBuscar = new JTextField(15);
        
        topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.add(btnCargarCsv);
        if(modoBusqueda){
            topPanel.add(txtBuscar);
            topPanel.add(btnBuscar);
            topPanel.add(btnRestablecer);
        }
        
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.add(topPanel, BorderLayout.WEST);
        headerPanel.add(lblTitulo, BorderLayout.CENTER);
        add(headerPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        
        // Estilo por defecto
        colorFondo = Color.WHITE;
        fuenteTexto = new Font("SansSerif", Font.PLAIN, 12);
        colorTexto = Color.BLACK;
        titulo = "Panel para mostrar tabla en archivo CSV";
        
        table.setBackground(colorFondo);
        table.setFont(fuenteTexto);
        table.setForeground(colorTexto);

        // Acción del botón para cargar CSV
        btnCargarCsv.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            int resultado = chooser.showOpenDialog(this);
            if (resultado == JFileChooser.APPROVE_OPTION) {
                File file = chooser.getSelectedFile();
                cargarArchivoCSV(file); // cargar y mostrar CSV
            }
        });
                
        //Acción del botón para buscar elemento
        btnBuscar.addActionListener(e-> {
            if(todosLosDatos.isEmpty()){
                JOptionPane.showMessageDialog(this, "No hay datos para mostrar en el panel", "Advertencia", JOptionPane.WARNING_MESSAGE);
                return;
            }
            String palabra = txtBuscar.getText().trim();
            resaltarFilas(palabra);  
        });
        
        //Acción del botón restablecer para volver a la tabla original
        btnRestablecer.addActionListener(e -> {
            if(todosLosDatos.isEmpty()){
                JOptionPane.showMessageDialog(this, "No hay datos por restablecer en el panel ", "Advertencia", JOptionPane.WARNING_MESSAGE);
                return;
            }
            todosLosDatos = new ArrayList<>(datosOriginales);
            paginaActual = 0;
            ordenarEstados.clear(); // Reiniciar estados de orden
            txtBuscar.setText("");
            remove(pnlPaginacion);
            if (todosLosDatos.size() > filasPorPagina) {
                construirPaginacion();
                add(pnlPaginacion, BorderLayout.SOUTH);
            }
            actualizarPaginaTabla();
        });

        
        //Evento de ordenamiento por encabezado
        table.getTableHeader().addMouseListener(new MouseAdapter(){
            @Override
            public void mouseClicked(MouseEvent e){
                int col = table.columnAtPoint(e.getPoint());
                if(col >= 0 && encabezadosColumna != null){
                    ordenarPorColumna(col);   
                }             
            } 
        });
    }

    /**
    * Carga y muestra los datos proporcionados como encabezados y filas.
    * Activa paginación si el número de filas excede el límite configurado.
    *
    * @param encabezados Arreglo de nombres de columnas.
    * @param datos Matriz de datos (cada fila es un arreglo de Strings).
    */
    public void setDatos(String[] encabezados, String[][] datos) {
        this.encabezadosColumna = encabezados;
        this.datosOriginales = new ArrayList<>();
        this.todosLosDatos = new ArrayList<>();
        Collections.addAll(this.datosOriginales, datos);
        Collections.addAll(this.todosLosDatos, datos);
        
        this.paginaActual = 0;
        calcularFilasPorPagina();

        remove(pnlPaginacion);
        if (todosLosDatos.size() > filasPorPagina) {
            construirPaginacion();
            add(pnlPaginacion, BorderLayout.SOUTH);
        }

        actualizarPaginaTabla();
    }
    
    /**
    * Calcula el número de filas por página con base en la opción configurada.
    * Opciones en la propiedad solo de 5, 10, 15 y 20 filas.
    * Si la opción es "Otro", usa el valor personalizado por el usuario.
    */
    private void calcularFilasPorPagina(){
        switch(opcionFilasPorPagina){
            case "5": filasPorPagina = 5; break;
            case "10": filasPorPagina = 10; break;
            case "15": filasPorPagina = 15; break;
            case "20": filasPorPagina = 20; break;
            case "Otro": filasPorPagina = filasPorPaginaPersonalizado; break;
            default: filasPorPagina = 20;
        }
    }
    
    /**
    * Actualiza la tabla para mostrar solo los datos de la página actual.
    * Se utiliza internamente en la paginación.
    */
    private void actualizarPaginaTabla() {
        int inicio = paginaActual * filasPorPagina;
        int fin = Math.min(inicio + filasPorPagina, todosLosDatos.size());

        String[][] datosPagina = new String[fin - inicio][encabezadosColumna.length];
        for (int i = inicio; i < fin; i++) {
            datosPagina[i - inicio] = todosLosDatos.get(i);
        }

        DefaultTableModel model = new DefaultTableModel(datosPagina, encabezadosColumna);
        table.setModel(model);
    }

    /**
    * Construye el panel de paginación con botones para navegar entre páginas.
    */
    private void construirPaginacion() {
        pnlPaginacion.removeAll();
        int paginasTotales = (int) Math.ceil((double) todosLosDatos.size() / filasPorPagina);

        JButton btnPrevio = new JButton("«");
        btnPrevio.addActionListener(e -> {
            if (paginaActual > 0) {
                paginaActual--;
                actualizarPaginaTabla();
            }
        });
        pnlPaginacion.add(btnPrevio);

        for (int i = 0; i < paginasTotales; i++) {
            int indicePagina = i;
            JButton btnPagina = new JButton(String.valueOf(i + 1));
            btnPagina.addActionListener(e -> {
                paginaActual = indicePagina;
                actualizarPaginaTabla();
            });
            pnlPaginacion.add(btnPagina);
        }

        JButton btnSiguiente = new JButton("»");
        btnSiguiente.addActionListener(e -> {
            if (paginaActual < paginasTotales - 1) {
                paginaActual++;
                actualizarPaginaTabla();
            }
        });
        pnlPaginacion.add(btnSiguiente);
    }

    /**
    * Carga un archivo CSV desde el sistema de archivos, lo analiza y muestra su contenido en la tabla.
    *
    * @param file Archivo CSV seleccionado por el usuario.
    */
    private void cargarArchivoCSV(File file) {
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            List<String[]> filas = new ArrayList<>();
            String linea;
            while ((linea = br.readLine()) != null) {
                filas.add(linea.split(",")); // Separación básica
            }
            if (!filas.isEmpty()) {
                String[] encabezados = filas.get(0);
                String[][] datos = filas.subList(1, filas.size()).toArray(new String[0][]);
                setDatos(encabezados, datos);
            } else {
                JOptionPane.showMessageDialog(this, "El archivo está vacío", "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Error al leer el archivo: ", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    /**
    * Filtra las filas que contienen exactamente el valor buscado (sin distinguir mayúsculas).
    * Solo se mostrarán las filas que contengan al menos una celda con coincidencia exacta.
    *
    * @param palabra Palabra clave que se desea buscar en la tabla.
    */
    private void resaltarFilas(String palabra) {
        if(palabra == null || palabra.isEmpty()){
            JOptionPane.showMessageDialog(this, "Escribe un valor para buscar.", "Advertencia", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        List<String[]> resultados = new ArrayList<>();
        
        for (String[] fila : datosOriginales) {
            for (String celda : fila) {
                if (celda != null && celda.equalsIgnoreCase(palabra)) {
                    resultados.add(fila);
                    break; // Salta a la siguiente fila si una celda coincide
                }
            }
        }

        if (resultados.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No se encontraron coincidencias.", "Búsqueda", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        todosLosDatos = resultados;
        paginaActual = 0;

        remove(pnlPaginacion);
        if (todosLosDatos.size() > filasPorPagina) {
            construirPaginacion();
            add(pnlPaginacion, BorderLayout.SOUTH);
        }

        actualizarPaginaTabla();
    }
    
    /**
    * Ordena la tabla por la columna especificada según un ciclo:
    * ascendente → descendente → orden original → ascendente...
    *
    * @param indiceColumna Índice de la columna a ordenar.
    */
    private void ordenarPorColumna(int indiceColumna){
        int estado = ordenarEstados.getOrDefault(indiceColumna, 0);
        
        Comparator<String[]> comparador = Comparator.comparing(row -> row[indiceColumna]);
        
        switch(estado){
            case 0 -> todosLosDatos.sort(comparador);
            case 1 -> todosLosDatos.sort(comparador.reversed());
            case 2 -> todosLosDatos = new ArrayList<>(datosOriginales);
        }
        
        ordenarEstados.put(indiceColumna, (estado + 1)%3);
        paginaActual = 0;
        actualizarPaginaTabla();
    }

    // Getters y setters para uso en NetBeans (propiedades)
    
    /**
    * Obtiene el color de fondo actual de la tabla.
    * @return Color de fondo asignado.
    */
    public Color getColorFondo() {
        return colorFondo;
    }
    
    /**
    * Establece el color de fondo de la tabla.
    *
    * @param colorFondo Color a aplicar.
    */
    public void setColorFondo(Color colorFondo) {
        this.colorFondo = colorFondo;
        table.setBackground(colorFondo);
        scrollPane.setBackground(colorFondo);
    }

    /**
    * Obtiene la fuente de texto actual usada en la tabla.
    * @return Fuente actual del texto.
    */
    public Font getFuenteTexto() {
        return fuenteTexto;
    }
    
    /**
    * Establece la fuente del texto en la tabla.
    *
    * @param fuenteTexto Fuente que se aplicará.
    */
    public void setFuenteTexto(Font fuenteTexto) {
        this.fuenteTexto = fuenteTexto;
        table.setFont(fuenteTexto);
    }

    /**
    * Obtiene el color de texto actual de la tabla.
    * @return Color del texto asignado.
    */
    public Color getColorTexto() {
        return colorTexto;
    }
    
    /**
    * Establece el color del texto de la tabla.
    *
    * @param colorTexto Color del texto.
    */
    public void setColorTexto(Color colorTexto) {
        this.colorTexto = colorTexto;
        table.setForeground(colorTexto);
    }
    
    /**
    * Obtiene el nombre de título  de la tabla.
    * @return String del título asignado.
    */
    public String getTitulo(){
        return titulo;
    }
    
    /**
    * Establece el título que se muestra encima de la tabla.
    *
    * @param titulo Texto a mostrar como título.
    */
    public void setTitulo(String titulo){
        lblTitulo.setText(titulo);
        lblTitulo.setFont(new Font("Arial", Font.BOLD, 16));
    }
    
    /**
    * Verifica si el modo búsqueda está habilitado.
    * @return true si está activado, false si no.
    */
    public boolean isModoBusqueda(){
        return modoBusqueda;
    }
    
    /**
    * Define si el modo de búsqueda está activado o no.
    * Esta propiedad se puede modificar desde el editor visual de NetBeans.
    *
    * @param modoBusqueda true para habilitar búsqueda; false para deshabilitarla.
    */
    public void setModoBusqueda(boolean modoBusqueda){
        this.modoBusqueda = modoBusqueda;
    }
    
    /**
    * Obtiene la opción actual seleccionada para el número de filas por página.
    * @return Cadena que representa la opción seleccionada ("5", "10", "15", "20", "Otro").
    */
    public String getOpcionFilasPorPagina(){
        return opcionFilasPorPagina;
    }
    
    /**
    * Establece la opción de filas por página ("5", "10", "15", "20", "Otro").
    *
    * @param opcionFilasPorPagina Opción elegida como cadena.
    */
    public void setOpcionFilasPorPagina(String opcionFilasPorPagina){
        this.opcionFilasPorPagina = opcionFilasPorPagina;
    }
    
    /**
    * Obtiene el valor personalizado de filas por página si se seleccionó la opción "Otro".
    * @return Número entero que representa las filas por página configuradas manualmente.
    */
    public int getFilasPorPaginaPersonalizado(){
        return filasPorPaginaPersonalizado;
    }
    
    /**
    * Define cuántas filas por página se mostrarán si la opción es "Otro".
    *
    * @param filasPorPaginaPersonalizado Número de filas definido por el usuario.
    */
    public void setFilasPorPaginaPersonalizado(int filasPorPaginaPersonalizado){
        this.filasPorPaginaPersonalizado = filasPorPaginaPersonalizado;
    }
}

