package exchange_rate;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.json.JSONObject;

public class Main {

    private static final Logger logger = Logger.getLogger(Main.class);

    static {
        String log4jConfigPath = "C:\\Users\\Win10\\eclipse-workspace\\exchange_rate\\Properties\\log4j.properties";
        PropertyConfigurator.configure(log4jConfigPath);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Exchange Rates Fetcher");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(600, 400);

            // Panel główny
            JPanel panel = new JPanel();
            panel.setLayout(new BorderLayout());

            // Górny panel z polem wyboru źródła danych
            JPanel topPanel = new JPanel();
            JLabel label = new JLabel("Select Data Source:");
            String[] dataSources = {"NBP", "ExchangeRate API", "Open Exchange Rates"};
            JComboBox<String> dataSourceComboBox = new JComboBox<>(dataSources);
            topPanel.add(label);
            topPanel.add(dataSourceComboBox);

            // Środkowy panel z obszarem tekstowym do wyświetlania wyników
            JTextArea textArea = new JTextArea();
            textArea.setEditable(false);
            JScrollPane scrollPane = new JScrollPane(textArea);

            // Dolny panel z przyciskiem pobierania kursów
            JPanel bottomPanel = new JPanel();
            JButton fetchButton = new JButton("Fetch Exchange Rates");
            bottomPanel.add(fetchButton);

            // Dodawanie paneli do głównego panelu
            panel.add(topPanel, BorderLayout.NORTH);
            panel.add(scrollPane, BorderLayout.CENTER);
            panel.add(bottomPanel, BorderLayout.SOUTH);

            // Dodawanie głównego panelu do ramki
            frame.add(panel);

            // Obsługa kliknięcia przycisku
            fetchButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    String dataSource = (String) dataSourceComboBox.getSelectedItem();
                    fetchExchangeRates(dataSource, textArea);
                }
            });

            // Wyświetlenie okna
            frame.setVisible(true);
        });
    }

    private static void fetchExchangeRates(String dataSource, JTextArea textArea) {
        try {
            URL url;
            switch (dataSource) {
                case "NBP":
                    url = new URL("http://api.nbp.pl/api/exchangerates/tables/A/");
                    break;
                case "ExchangeRate API":
                    url = new URL("https://v6.exchangerate-api.com/v6/8e507997dc413b9f1858668b/latest/USD");
                    break;
                case "Open Exchange Rates":
                    url = new URL("https://openexchangerates.org/api/latest.json?app_id=9ce7f33638c547369cbf171d54d9d84e");
                    break;
                default:
                    textArea.setText("Unknown data source selected.");
                    return;
            }

            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            con.setRequestProperty("User-Agent", "Mozilla/5.0");
            con.setRequestProperty("Accept", "application/json");

            if (con.getResponseCode() == 200) {
                BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
                String inputLine;
                StringBuilder content = new StringBuilder();
                while ((inputLine = in.readLine()) != null) {
                    content.append(inputLine);
                }
                in.close();

                // Przetwarzanie danych
                String result;
                switch (dataSource) {
                    case "NBP":
                        result = parseNBPResponse(content.toString());
                        break;
                    case "ExchangeRate API":
                        result = parseExchangeRateAPIResponse(content.toString());
                        break;
                    case "Open Exchange Rates":
                        result = parseOpenExchangeRatesResponse(content.toString());
                        break;
                    default:
                        result = "Unknown data source.";
                }
                textArea.setText(result);
                logger.info("Exchange rates fetched successfully from " + dataSource);
            } else {
                String errorMessage = "Error: " + con.getResponseCode() + " " + con.getResponseMessage();
                textArea.setText(errorMessage);
                logger.error(errorMessage);
            }
        } catch (Exception e) {
            String errorMessage = "An error occurred: " + e.getMessage();
            textArea.setText(errorMessage);
            logger.error(errorMessage, e);
        }
    }

    private static String parseNBPResponse(String jsonResponse) {
        StringBuilder result = new StringBuilder("NBP Exchange Rates:\n");
        Pattern pattern = Pattern.compile("\"currency\":\"(.*?)\",\"code\":\"(.*?)\",\"mid\":(.*?)}");
        Matcher matcher = pattern.matcher(jsonResponse);
        while (matcher.find()) {
            result.append(String.format("%-15s %s\n", matcher.group(1), matcher.group(3)));
        }
        return result.toString();
    }

    private static String parseExchangeRateAPIResponse(String jsonResponse) {
        StringBuilder result = new StringBuilder("ExchangeRate API Exchange Rates:\n");
        try {
            JSONObject jsonObject = new JSONObject(jsonResponse);
            JSONObject rates = jsonObject.getJSONObject("conversion_rates");

            for (String key : rates.keySet()) {
                result.append(String.format("%-15s %s\n", key, rates.get(key)));
            }
        } catch (Exception e) {
            result.append("Error parsing ExchangeRate API response: ").append(e.getMessage());
            logger.error("Error parsing ExchangeRate API response", e);
        }

        return result.toString();
    }

    private static String parseOpenExchangeRatesResponse(String jsonResponse) {
        StringBuilder result = new StringBuilder("Open Exchange Rates:\n");

        try {
            JSONObject jsonObject = new JSONObject(jsonResponse);
            JSONObject rates = jsonObject.getJSONObject("rates");

            for (String key : rates.keySet()) {
                result.append(String.format("%-15s %s\n", key, rates.get(key)));
            }
        } catch (Exception e) {
            result.append("Error parsing Open Exchange Rates response: ").append(e.getMessage());
            logger.error("Error parsing Open Exchange Rates response", e);
        }

        return result.toString();
    }
}
