package sambal.ontologysambal;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.*;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.vocabulary.OWL2;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;

public class OntologiService {

    public static final String NS =
            "http://www.semanticweb.org/aulia/ontologies/2026/8/untitled-ontology-4#";

    private static final String PREFIX = "PREFIX : <" + NS + ">\n"
            + "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
            + "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n";

    private static final OntologiService INSTANCE = new OntologiService();

    public static OntologiService get() {
        return INSTANCE;
    }

    private final Model model = ModelFactory.createDefaultModel();

    private OntologiService() {
        try (InputStream in = getClass().getResourceAsStream("/ontologi.owl")) {
            if (in == null) {
                throw new IllegalStateException("ontologi.owl tidak ada di src/main/resources");
            }
            RDFDataMgr.read(model, in, Lang.RDFXML);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Property p(String local) {
        return model.createProperty(NS + local);
    }

    private Resource r(String local) {
        return model.createResource(NS + local);
    }

    /** "CabaiRawitMerah" -> "Cabai Rawit Merah" */
    static String pretty(String s) {
        if (s == null) return "";
        return s.replaceAll("(?<=[a-z])(?=[A-Z])", " ").trim();
    }

    private String label(Resource res) {
        Statement st = res.getProperty(RDFS.label);
        String s = (st != null) ? st.getString() : res.getLocalName();
        return pretty(s);
    }

    private String lit(Resource s, String prop) {
        Statement st = s.getProperty(p(prop));
        return st == null ? "" : st.getString().trim();
    }

    private static String cap(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private static int level(String label) {
        String l = label.toLowerCase();
        if (l.contains("sangat")) return 3;
        if (l.contains("sedang")) return 2;
        return 1;
    }

    private static Map<String, String> opt(String id, String label) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("id", id);
        m.put("label", label);
        return m;
    }

    private List<String> labelsOf(Resource s, String prop) {
        List<String> out = new ArrayList<>();
        for (NodeIterator it = model.listObjectsOfProperty(s, p(prop)); it.hasNext();) {
            RDFNode n = it.next();
            if (n.isResource()) out.add(label(n.asResource()));
        }
        Collections.sort(out);
        return out;
    }

    private List<String> lines(String text, String splitRegex) {
        List<String> out = new ArrayList<>();
        if (text == null || text.isBlank()) return out;
        for (String x : text.trim().split(splitRegex)) {
            x = x.trim();
            if (!x.isEmpty()) out.add(x);
        }
        return out;
    }

    private static boolean blank(String s) {
        return s == null || s.isBlank();
    }

    /* ---------------- isi dropdown filter ---------------- */

    public Map<String, Object> opsi() {
        Map<String, Object> o = new LinkedHashMap<>();

        o.put("jenis", List.of(
                opt(NS + "SambalMatang", "Matang"),
                opt(NS + "SambalMentah", "Mentah")));

        List<Resource> tk = new ArrayList<>();
        for (ResIterator it = model.listResourcesWithProperty(RDF.type, r("TingkatKepedasan")); it.hasNext();) {
            tk.add(it.next());
        }
        tk.sort(Comparator.comparingInt(x -> level(label(x))));
        List<Map<String, String>> kp = new ArrayList<>();
        for (Resource x : tk) kp.add(opt(x.getURI(), label(x)));
        o.put("pedas", kp);

        List<Map<String, String>> bp = new ArrayList<>();
        String q = PREFIX + "SELECT DISTINCT ?b WHERE { ?s :MenggunakanBahanPedas ?b }";
        try (QueryExecution qe = QueryExecutionFactory.create(q, model)) {
            ResultSet rs = qe.execSelect();
            while (rs.hasNext()) {
                Resource b = rs.next().getResource("b");
                bp.add(opt(b.getURI(), label(b)));
            }
        }
        bp.sort(Comparator.comparing(m -> m.get("label")));
        o.put("bahan", bp);

        o.put("sajian", List.of(
                opt(NS + "MakananBerat", "Makanan Berat"),
                opt(NS + "Cemilan", "Cemilan")));
        return o;
    }

    /* ---------------- daftar sambal (SPARQL) ---------------- */

    public List<Map<String, Object>> daftar(String cari, String jenis, String pedas,
                                            String bahan, String sajian) {
        StringBuilder q = new StringBuilder(PREFIX);
        q.append("SELECT DISTINCT ?s ?nama ?asal ?jenis ?tk WHERE {\n");
        q.append("  VALUES ?jenis { :SambalMatang :SambalMentah }\n");
        q.append("  ?s a ?jenis .\n");
        q.append("  OPTIONAL { ?s :NamaSambal ?nama }\n");
        q.append("  OPTIONAL { ?s :AsalDaerah ?asal }\n");
        q.append("  OPTIONAL { ?s :MemilikiKepedasan ?tk }\n");
        if (!blank(jenis))  q.append("  FILTER(?jenis = ?fJenis)\n");
        if (!blank(pedas))  q.append("  FILTER EXISTS { ?s :MemilikiKepedasan ?fPedas }\n");
        if (!blank(bahan))  q.append("  FILTER EXISTS { ?s :MenggunakanBahanPedas ?fBahan }\n");
        if (!blank(sajian)) q.append("  FILTER EXISTS { ?s :DisajikanUntuk ?d . ?d a ?fSajian }\n");
        if (!blank(cari)) {
            q.append("  FILTER(\n");
            q.append("    CONTAINS(REPLACE(LCASE(STR(?nama)),\"[^a-z0-9]\",\"\"), ?fCari)\n");
            q.append(" || CONTAINS(REPLACE(LCASE(STR(?asal)),\"[^a-z0-9]\",\"\"), ?fCari)\n");
            q.append(" || EXISTS { ?s ?rel ?o . ?o rdfs:label ?lbl .\n");
            q.append("      FILTER(CONTAINS(REPLACE(LCASE(STR(?lbl)),\"[^a-z0-9]\",\"\"), ?fCari)) }\n");
            q.append(" || EXISTS { ?s :RasaSambal|:TeksturSambal|:WarnaSambal ?lit .\n");
            q.append("      FILTER(CONTAINS(REPLACE(LCASE(STR(?lit)),\"[^a-z0-9]\",\"\"), ?fCari)) }\n");
            q.append("  )\n");
        }
        q.append("}\nORDER BY ?nama");

        ParameterizedSparqlString pss = new ParameterizedSparqlString(q.toString());
        if (!blank(jenis))  pss.setIri("fJenis", jenis);
        if (!blank(pedas))  pss.setIri("fPedas", pedas);
        if (!blank(bahan))  pss.setIri("fBahan", bahan);
        if (!blank(sajian)) pss.setIri("fSajian", sajian);
        if (!blank(cari))   pss.setLiteral("fCari", cari.toLowerCase().replaceAll("[^a-z0-9]", ""));

        List<Map<String, Object>> out = new ArrayList<>();
        try (QueryExecution qe = QueryExecutionFactory.create(pss.asQuery(), model)) {
            ResultSet rs = qe.execSelect();
            while (rs.hasNext()) {
                QuerySolution qs = rs.next();
                Resource s = qs.getResource("s");
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", s.getURI());
                m.put("nama", pretty(qs.contains("nama") ? qs.getLiteral("nama").getString().trim() : label(s)));
                m.put("asal", qs.contains("asal") ? qs.getLiteral("asal").getString().trim() : "");
                m.put("jenis", qs.getResource("jenis").getLocalName().replace("Sambal", ""));
                if (qs.contains("tk")) {
                    String l = label(qs.getResource("tk"));
                    m.put("kepedasan", l);
                    m.put("level", level(l));
                } else {
                    m.put("kepedasan", "");
                    m.put("level", 0);
                }
                out.add(m);
            }
        }
        return out;
    }

    /* ---------------- detail 1 sambal ---------------- */

    public Map<String, Object> detail(String iri) {
        if (blank(iri)) return null;
        Resource s = model.getResource(iri);
        if (!model.containsResource(s)) return null;

        Map<String, Object> d = new LinkedHashMap<>();
        d.put("id", iri);
        String nama = lit(s, "NamaSambal");
        d.put("nama", pretty(nama.isEmpty() ? label(s) : nama));
        d.put("jenis", s.hasProperty(RDF.type, r("SambalMentah")) ? "Mentah" : "Matang");
        d.put("asal", lit(s, "AsalDaerah"));
        d.put("rasa", lit(s, "RasaSambal"));
        d.put("tekstur", lit(s, "TeksturSambal"));
        d.put("warna", lit(s, "WarnaSambal"));
        d.put("lama", lit(s, "LamaProses"));

        List<String> kp = labelsOf(s, "MemilikiKepedasan");
        String kpl = kp.isEmpty() ? "" : kp.get(0);
        d.put("kepedasan", kpl);
        d.put("level", kpl.isEmpty() ? 0 : level(kpl));

        d.put("bahanPedas", labelsOf(s, "MenggunakanBahanPedas"));
        d.put("bahanPenyedap", labelsOf(s, "MenggunakanBahanPenyedap"));

        List<String> takaran = new ArrayList<>();
        for (String l : lines(lit(s, "TakaranBahan"), "\\R+")) {
            takaran.add(l.replaceFirst("^[-•*]\\s*", ""));
        }
        d.put("takaran", takaran);

        // properti langkah pembuatan IRI-nya dari WebProtege, dicari lewat label "LangkahPembuatan"
        List<String> langkah = new ArrayList<>();
        for (ResIterator it = model.listSubjectsWithProperty(RDFS.label, "LangkahPembuatan"); it.hasNext();) {
            Resource pr = it.next();
            Statement st = s.getProperty(model.createProperty(pr.getURI()));
            if (st != null) {
                for (String l : lines(st.getString(), "\\R+")) {
                    langkah.add(cap(l.replaceFirst("^\\d+[.)]\\s*", "")));
                }
            }
        }
        d.put("langkah", langkah);

        List<String> alat = new ArrayList<>();
        for (String a : lines(lit(s, "NamaAlat"), "[\\n\\r,;]+")) alat.add(cap(a));
        d.put("alat", alat);

        List<String> teknik = new ArrayList<>();
        teknik.addAll(labelsOf(s, "MenggunakanTeknikPenghancuran"));
        teknik.addAll(labelsOf(s, "MenggunakanTeknikPemasakan"));
        teknik.addAll(labelsOf(s, "MenggunakanTeknikPengawetan"));
        d.put("teknik", teknik);
        d.put("teknikDesc", lit(s, "NamaTeknik"));

        List<Map<String, String>> sajian = new ArrayList<>();
        for (NodeIterator it = model.listObjectsOfProperty(s, p("DisajikanUntuk")); it.hasNext();) {
            RDFNode n = it.next();
            if (!n.isResource()) continue;
            Resource dish = n.asResource();
            String kat = dish.hasProperty(RDF.type, r("Cemilan")) ? "Cemilan" : "Makanan Berat";
            sajian.add(opt(label(dish), kat));
        }
        sajian.sort(Comparator.comparing(m -> m.get("id")));
        d.put("sajian", sajian);

        List<List<String>> triples = new ArrayList<>();
        for (StmtIterator it = s.listProperties(); it.hasNext();) {
            Statement st = it.next();
            if (st.getPredicate().equals(RDF.type) && OWL2.NamedIndividual.equals(st.getObject())) continue;
            triples.add(List.of(qname(s), qname(st.getPredicate()), objText(st.getObject())));
        }
        d.put("triples", triples);
        return d;
    }

    private String qname(Resource res) {
        if (res.isAnon()) return "_:blank";
        String uri = res.getURI();
        if (uri.startsWith(NS)) return ":" + uri.substring(NS.length());
        String sf = model.shortForm(uri);
        if (sf.startsWith("http")) {
            Statement l = res.getProperty(RDFS.label);
            return ":" + (l != null ? l.getString() : res.getLocalName());
        }
        return sf;
    }

    private String objText(RDFNode n) {
        if (n.isResource()) return qname(n.asResource());
        String t = n.asLiteral().getLexicalForm().replaceAll("\\s+", " ").trim();
        return t.length() > 80 ? t.substring(0, 80) + "…" : t;
    }
}