package net.tetrakoopa.gradle.plugin.shell.documentation;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class DocumentationToMarkdownGenerator {
    
    private static final Map<String, Map<String, Map<String, String>>> styles = new HashMap<>();
    
    static {
        // Initialisation des styles similaire au script GAWK
        Map<String, Map<String, String>> githubStyle = new HashMap<>();
        
        // Configuration des styles pour github
        addStyle(githubStyle, "h1", ".*", "## &");
        addStyle(githubStyle, "h2", ".*", "### &");
        addStyle(githubStyle, "h3", ".*", "#### &");
        addStyle(githubStyle, "code", ".*", "~~~&");
        addStyle(githubStyle, "/code", "", "~~~");
        addStyle(githubStyle, "argN", "^([0-9]+)[ \\t]+(\\S+)", "**$1** _$2_:");
        addStyle(githubStyle, "arg@", "^(\\S+)", "**...** _$1_:");
        addStyle(githubStyle, "li", ".*", "* &");
        addStyle(githubStyle, "anchor", ".*", "[&](#&)");
        addStyle(githubStyle, "exitcode", "([><!]?[0-9]+)([ \\t]+(.*))?", "**`$1`** : $3");
        addStyle(githubStyle, "exitcode-range", "(\\[([0-9]+)(-([0-9]+))\\])([ \\t]+(.*))?", "**`$2`➔`$4`** : $6");
        
        styles.put("github", githubStyle);
    }
    
    private static void addStyle(Map<String, Map<String, String>> style, String type, 
                               String from, String to) {
        Map<String, String> typeMap = new HashMap<>();
        typeMap.put("from", from);
        typeMap.put("to", to);
        style.put(type, typeMap);
    }
    
    public static void convert(InputStream input, OutputStream output) throws IOException {
        convert(input, output, "github");
    }
    
    public static void convert(InputStream input, OutputStream output, String style) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(output, StandardCharsets.UTF_8));
        
        ConverterState state = new ConverterState(style);
        String line;
        
        while ((line = reader.readLine()) != null) {
            state.processLine(line);
        }
        
        // Écrire le résultat final
        state.writeOutput(writer);
        
        writer.flush();
    }
    
    private static class ConverterState {
        private final String style;
        private int methodCount = 0;
        private int argCount = 0;
        
        private boolean inDescription = false;
        private boolean inExample = false;
        private boolean inStdout = false;
        
        private boolean hasArgs = false;
        private boolean hasExample = false;
        private boolean hasStdin = false;
        private boolean hasExitcode = false;
        private boolean hasStdout = false;
        
        private StringBuilder docblock = new StringBuilder();
        private StringBuilder toc = new StringBuilder();
        private StringBuilder doc = new StringBuilder();
        private String functionName = "";
        
        public ConverterState(String style) {
            this.style = style;
        }
        
        public void processLine(String line) {
            // @description
            if (line.startsWith("# @description")) {
                inDescription = true;
                initMethod();
                processDescription(line);
                return;
            }
            
            if (inDescription) {
                if (!line.startsWith("#") || (line.startsWith("# @") && !line.startsWith("# @description"))) {
                    inDescription = false;
                } else {
                    processDescription(line);
                    return;
                }
            }
            
            // @example
            if (line.startsWith("# @example")) {
                inExample = true;
                docblock.append("\n").append(render("h3", "Example"));
                docblock.append("\n\n").append(render("code", "bash"));
                return;
            }
            
            if (inExample) {
                if (!line.startsWith("#   ")) {
                    inExample = false;
                    docblock.append("\n").append(render("/code", "")).append("\n");
                } else {
                    docblock.append("\n").append(line.substring(4));
                    return;
                }
            }
            
            // @arg
            if (line.matches("^\\s*#\\s+@arg\\s+.*")) {
                initArg();
                String processed = line.replaceAll("^\\s*#\\s+@arg\\s+", "");
                processed = render("argN", argCount + " " + processed);
                argCount++;
                docblock.append(render("li", processed)).append("\n");
                return;
            }
            
            // @args
            if (line.matches("^\\s*#\\s+@args\\s+.*")) {
                initArg();
                String processed = line.replaceAll("^\\s*#\\s+@args\\s+", "");
                processed = render("arg@", processed);
                docblock.append(render("li", processed)).append("\n");
                return;
            }
            
            // @stdin
            if (line.startsWith("# @stdin")) {
                hasStdin = true;
                String processed = line.replaceAll("^# @stdin\\s+", "");
                docblock.append("\n").append(render("h2", "Read from stdin"));
                docblock.append("\n\n").append(render("li", processed)).append("\n");
                return;
            }
            
            // @exitcode
            if (line.matches("^\\s*#\\s+@exitcode\\s+.*")) {
                if (!hasExitcode) {
                    hasExitcode = true;
                    docblock.append("\n").append(render("h2", "Exit codes")).append("\n\n");
                }
                
                String processed = line.replaceAll("^#\\s*@exitcode\\s*", "");
                
                if (processed.matches("^\\s*\\[([0-9]+-[0-9]+\\]).*")) {
                    processed = render("exitcode-range", processed);
                } else {
                    processed = render("exitcode", processed);
                }
                
                docblock.append(render("li", processed)).append("\n");
                return;
            }
            
            // @see
            if (line.matches("^\\s*#\\s+@see\\s+.*")) {
                String processed = line.replaceAll("^\\s*#\\s+@see\\s+", "");
                processed = render("anchor", processed);
                processed = render("li", processed);
                docblock.append("\n").append(render("h3", "See also")).append("\n\n").append(processed).append("\n");
                return;
            }
            
            // @stdout
            if (line.matches("^\\s*#\\s+@stdout.*")) {
                if (!hasStdout) {
                    hasStdout = true;
                    inStdout = true;
                    docblock.append("\n").append(render("h2", "Output on stdout")).append("\n");
                }
                return;
            }
            
            if (inStdout) {
                if (!line.startsWith("#") || (line.startsWith("# @") && !line.startsWith("# @stdout"))) {
                    inStdout = false;
                } else {
                    String processed = line
                        .replaceAll("^\\s*#\\s+@stdout", "")
                        .replaceAll("^#\\s+", "")
                        .replaceAll("^#$", "");
                    docblock.append("\n").append(processed);
                    return;
                }
            }
            
            // Détection de fonction avec documentation
            if (line.matches("^\\s*(function\\s+)?([a-zA-Z_][a-zA-Z0-9_]*)\\s*(\\(\\s*\\))?\\s*\\{?\\s*$") 
                && docblock.length() > 0) {
                
                Matcher matcher = Pattern.compile("^\\s*(function\\s+)?([a-zA-Z_][a-zA-Z0-9_]*)\\s*(\\(\\s*\\))?\\s*\\{?\\s*$")
                    .matcher(line);
                
                if (matcher.find()) {
                    functionName = matcher.group(2);
                    
                    if (methodCount > 0) {
                        doc.append("\n----\n");
                    }
                    
                    methodCount++;
                    
                    doc.append("\n").append(render("h1", functionName)).append("\n").append(docblock);
                    
                    String url = functionName.replaceAll("\\W", "");
                    toc.append("\n").append("* [").append(functionName).append("](#").append(url).append(")");
                    
                    docblock = new StringBuilder();
                }
            }
        }
        
        private void processDescription(String line) {
            String processed = line
                .replaceAll("^# @description\\s+", "")
                .replaceAll("^#\\s+", "")
                .replaceAll("^#$", "");
            docblock.append("\n").append(processed);
        }
        
        private void initArg() {
            if (!hasArgs) {
                hasArgs = true;
                argCount = 0;
                docblock.append("\n").append(render("h2", "Arguments")).append("\n\n");
            }
        }
        
        private void initMethod() {
            inExample = false;
            hasExample = false;
            hasArgs = false;
            hasStdin = false;
            hasExitcode = false;
            hasStdout = false;
            inStdout = false;
            docblock = new StringBuilder();
        }
        
        private String render(String type, String text) {
            Map<String, String> typeStyle = styles.get(style).get(type);
            if (typeStyle == null) {
                return text;
            }
            
            String from = typeStyle.get("from");
            String to = typeStyle.get("to");
            
            if (from == null || from.isEmpty()) {
                return to;
            }
            
            // Implémentation simplifiée de la substitution
            return text.replaceAll(from, to.replace("&", text).replace("$1", "$1").replace("$2", "$2"));
        }
        
        public void writeOutput(BufferedWriter writer) throws IOException {
            if (methodCount > 0) {
                // Note: script_name n'est pas disponible dans cette implémentation
                // mais pourrait être ajouté comme paramètre si nécessaire
                
                writer.write(toc.toString());
                writer.write("\n");
                writer.write(doc.toString());
            }
        }
    }
    
    // Méthode main pour tester
    public static void main(String[] args) {
        try {
            convert(System.in, System.out);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}