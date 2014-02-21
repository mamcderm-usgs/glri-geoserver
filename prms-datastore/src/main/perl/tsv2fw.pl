#!/usr/bin/perl

use strict;
use warnings;

my $infile = shift or die "Must supply input file\n";
open(FILE, "<$infile");
while (my $line = <FILE>) {
	if ($line =~ m/^#/) {
		$line =~ s/\r\n//;
		print "$line\n";
	} else {
		if ($line =~ m/^(\w+\s+)+\w+/) {
			# var header
			$line =~ s/\r\n//;
			print "$line\n";
		} elsif ($line =~ m/^(\d+\w\s+)+\d+\w/) {
			$line =~ s/\r\n//;
			print "$line\n";
		} elsif ($line =~ m/^(\d{4}-\d{2}-\d{2}:\d{2}:\d{2}:\d{2})\s+\d+(\s+[\d\.]+)+/) {
			my @elements = split(/\s+/, $line);
			my $len = @elements;
			printf("%s\t", $elements[0]);
			printf("%6d", $elements[1]);
			for (my $i=2; $i<$len; $i++) {
				printf("\t%1.6e", $elements[$i]);
			}
			print "\n";
		}
	}
}
