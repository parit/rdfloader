package swiss.sib;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Callable;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "Map", description = "loads the rdf/owl data into store/triples", mixinStandardHelpOptions = true)
public class CLIHandler
    implements Callable<Integer>
{
	@Option(names = {"-f", "--file"}, required = true, description = "rdf/owl file path")
	String rdfFile = "";
	
	@Option(names = {"-q", "--queryfile"}, required = true, description = "file with the sparql query in it")
	String queryFile = "";
	
	@Option (names = {"-o", "--out"}, required = true, description = "output file")
	String outfile = "";
	
	@Option (names = {"-d", "--delimitor"}, required = false, defaultValue = ";", description = "in-column delimitor to use. DEFAULT: ;")
	String incolseparator = "";
	
	public Integer call()
	    throws Exception
	{
		if (!Files.exists(Paths.get(rdfFile))) {
			throw new Exception ("RDF/OWL file specified doesn't exist: " + rdfFile );
		}
		if (!Files.exists(Paths.get(queryFile))) {
			throw new Exception ("Query file specified doesn't exist: " + rdfFile );
		}
		final Path path = Paths.get(outfile);
		if (Files.exists(path))
			Files.delete(path);
		return 0;
	}
}
