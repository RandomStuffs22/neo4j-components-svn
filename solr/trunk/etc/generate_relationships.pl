#!/usr/bin/perl

use strict;


my $no_users = 100;

my $max_connections = 10;

open (FILE, "+>relationships.xml");
print FILE "<add>\n";

for(my $i =0; $i < $no_users; $i++){

    print FILE "<doc>\n";

    my $rand_cons = int(rand($max_connections));
    
    if ($rand_cons ==0 ) {
	$rand_cons = 3;
    }
    my $cons = $i;
    for (my $y = 0; $y < $rand_cons; $y++) {

	my $r_user = int(rand($no_users));
	if ($r_user != $i) {
	    $cons .= ":$r_user";
	}
	
    }

    print FILE "<field name=\"id\">$i</field>\n";
    print FILE "<field name=\"neoRelationship\">$cons</field>\n";
    print FILE "</doc>\n";

}

print FILE "</add>\n";
close FILE;
