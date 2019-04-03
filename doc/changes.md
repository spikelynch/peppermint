# Changes

## Field names

The original code used full schema.org URLs for field and facet names, for example

    https___schema_org_publisher

The non-alphabetic characters normalised to '_' to make them valid Solr field names.

Facet names were of the form

    ${facet.field}_______${facet.target}________${facet.type}

where the delimiter is 7 underscores. The actual fields looked like:

    https___schema_org_keywords_______https___schema_org_Dataset_______facet_multi
    https___schema_org_publisher_______https___schema_org_Dataset_______facet

etc.  This is required so that the Angular components can split the facet field names up without breaking them at the underscores in the fake-URLs, but it was driving me crazy and makes the Solr results impossible to debug

I decided that the fields could just be 'keywords' without the full schema.org URLs, and trimming this off means that we can delimit the facet fields with single underscores (and change facet_multi to facetmulti):

    keywords_Dataset_facetmulti
    publisher_Dataset_facet

## fieldName for multiple facets

The addKvAndFacetsToDocument method in scripts/jsonld-parse/utils.groovy resolves links to
child items in fields like 'creator', but when I added this as a facet, the facets were getting the whole JSON object in them, rather than using the fieldName parameter from the facet. The code around 130 was only applying fieldName to single facets, I've reworked this so that it works with facetmulti, although it still probably needs some work.