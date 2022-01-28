package swiss.sib;

import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Iterator;
import java.util.Optional;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.RDFDataMgr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import picocli.CommandLine;

public class RDFLoader
{

	private static CLIHandler handler = new CLIHandler();
	private static final Logger logger = 
		LoggerFactory.getLogger(RDFLoader.class);
	
	public static void main(String[] args) throws Exception
	{
		logger.info("Starting application");
		new CommandLine(handler).execute(args);
		run();
		logger.info("Application finished");
	}
	
	private static void run() throws Exception {
		logger.debug("creating in memory rdf model");
		Model model = ModelFactory.createMemModelMaker().createFreshModel();
		// use the RDFDataMgr to find the input file
		try (InputStream in = RDFDataMgr.open(Paths.get(handler.rdfFile).toAbsolutePath().toString())) {
			model.read(in,null);
		};
		
		String queryString = Files.readString(Paths.get(handler.queryFile));
		var query = QueryFactory.create(queryString);
		logger.debug("Running query: \n" + queryString);
		
		try (
			PrintWriter pw = new PrintWriter(Files.newBufferedWriter(Paths.get(handler.outfile), StandardOpenOption.CREATE_NEW));
			QueryExecution qexec = QueryExecutionFactory.create(query, model))
		{
			logger.debug("Iterating over sparql results");
			ResultSet results = qexec.execSelect();
			Optional<Exception> findFirst = StreamSupport.stream(Spliterators.spliteratorUnknownSize(results, Spliterator.ORDERED), false)
				.map(el -> {
					final String id = el.getLiteral("id").getString();
					final String pid = sanitize(el.getLiteral("pid").getString());
					final String name = el.getLiteral("name").getString();
					final String chebi = el.getResource("chebi").getLocalName().replaceAll("_", ":");
					return new Tuple(id, pid, name, chebi);
				})
				.collect(Collectors.groupingBy(el -> el.rxnid))
				.entrySet()
				.stream()
				.map(el -> {
					final StringBuffer pids = new StringBuffer();
					final StringBuffer names = new StringBuffer();
					final StringBuffer chebis = new StringBuffer();
					Iterator<Tuple> itr = el.getValue().iterator();
					while(itr.hasNext()) {
						Tuple next = itr.next();
						pids.append(next.parid);
						names.append(next.parname);
						chebis.append(next.chebiid);
						if (itr.hasNext()) {
							pids.append(handler.incolseparator);
							names.append(handler.incolseparator);
							chebis.append(handler.incolseparator);
						}
					}
					try {
						pw.println(String.format("%s\t%s\t%s\t%s",el.getKey(), pids.toString(), names.toString(), chebis.toString()));
						return Optional.empty();
					} catch(Exception e) {
						return Optional.of(e);
					}
				})
				.filter(e -> e.isPresent())
				.map(e -> (Exception) e.get())
				.findFirst();
			if (findFirst.isPresent())
				throw findFirst.get();
			
			while (results.hasNext())
			{
				final QuerySolution next = results.next();
				final String reaction = next.getLiteral("id").getString();
				final String pid = next.getLiteral("pid").getString();
				final String name = next.getLiteral("name").getString();
				final String uri = next.getResource("chebi").getLocalName().replaceAll("_", ":");
				pw.println(String.format("%s\t%s\t%s\t%s", reaction, sanitize(pid),  name, uri));
			}
			pw.flush();
		}
		model.close();
	}
	
	private static String sanitize(String pid) {
		return pid.replaceAll("GENERIC:", "RHEA-COMP:").replaceAll("POLYMER:", "RHEA-COMP:");
	}
	
	
	private static class Tuple {
		String rxnid;
		String parid;
		String parname;
		String chebiid;
		
		Tuple(String rxnid, String parid, String parname, String chebiid) {
			this.rxnid = rxnid;
			this.parid = parid;
			this.parname = parname;
			this.chebiid = chebiid;
		}
	}

}
