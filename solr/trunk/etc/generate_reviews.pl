#!/usr/bin/perl
use strict;


my $file = "samples.xml";
my $no_users = 100;

my $contents;
open(FILE,"$file") || die "$0: cannot open $file $!\n";
while(<FILE>){

    $contents .= $_;
}

close FILE;

my @docs = split(/\<doc\>/, $contents);

shift (@docs);
my %ds;

foreach my $d (@docs) {

    my $id = getField($d, "id");
    my $lat = getField($d, "lat");
    my $lng = getField($d, "lng");
    my $business = getField($d, "cinema");

    my $listing = "<field name=\"business_name\">$business</field>\n".
	          "<field name=\"category\">cinema</field>\n".
		  "<field name=\"lat\">$lat</field>\n".
		  "<field name=\"lng\">$lng</field>\n";

    $ds{$id} = $listing;

}

my @outdocs;
my $no_docs = scalar(@docs);

my @reviews = ('great popcorn', 'lots of candy', 'good screen size', 'tons of stuff to do while waiting', 'attentive staff',
	       'plenty of parking', 'lots of late shows', 'rubbish movies', 'crap staff', 'will never go here again',
	       'no air cond','always out of butter');


my $no_reviews = scalar(@reviews);

open(FILE, "+>reviews.xml");
print FILE "<add>\n";
for (my $i =0; $i < $no_users; $i++) {

    my $doc_id = int(rand($no_docs));

    my $review_id = int(rand($no_reviews));

    my $d = $ds{$doc_id};
    $d .= "<field name=\"review\">".$reviews[$review_id]."</field>\n".
	"<field name=\"neoUserId\">".$i."</field>\n".
	"<field name=\"id\">$i</field>\n";

    
    print FILE "<doc>\n$d </doc>\n";

}

print FILE "</add>\n";
close FILE;






sub getField() {

    my $doc = shift;
    my $f = shift;

    $doc =~ m/name=\"$f\"\>(.*?)\</;

    return $1;

}
