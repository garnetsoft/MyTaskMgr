…or push an existing repository from the command line


git remote add origin https://github.com/garnetsoft/MyTaskMgr.git
git push -u origin master

#!/home/tptools/Linux/x86_64/bin/perl -W

use strict;
use Kx;
use Data::Dumper;
use Getopt::Long;
use IO::File; 
use File::Basename;
use File::Spec::Functions qw(rel2abs);
use MIME::Lite;
use Scalar::Util qw(looks_like_number);
use Number::Format;
use Date::Format;

use MIME::Lite::TT;
use MIME::Lite::TT::HTML;

my $de = new Number::Format(-thousands_sep   => ',',
                            -decimal_point   => '.',
                            -int_curr_symbol => '$');

usage () if (!defined $ARGV[0]);

my $curDir = dirname(rel2abs($0));
my $configFile = $ARGV[0];

# use input dateStr if any
my $dateStr = $ARGV[1];  
print "haha: $dateStr \n";

print "xxxx Base dir: $curDir, config: $configFile, date: $dateStr \n";

# parse config file
my $configs = parseConf($ARGV[0]);
my $logDir = $configs->{'logDir'};
mkdir $logDir if (! -d $logDir);


# setup html 
my $content = "<HTML><head>";
# add style sheet 
#$content .= "<style type=text/css> TD{font-family: Arial; font-size: 8pt;} table { border-collapse:collapse; } table, td , th { border:1px solid black; } th { background-color:green; color:white; } td {text-align:right;} </style>";
$content .= "<style type=text/css> TD{font-family: Arial; font-size: 8pt;} table { border-collapse:collapse; } table, td , th { border:1px solid black; } th { background-color:green; color:white; } td {text-align:right;} .ExternalClass * {line-height: 100%} </style>";
$content .= "</head><body>";

# Kdb properties
my $khost = $configs->{'khost'};
my $kport = $configs->{'kport'};
my $kuserpass = $configs->{'kuserpass'};
my $ktable = $configs->{'ktable'};
my $ksqlfile = $configs->{'ksql'};

# section names
my $sectionNames = $configs -> {'sections'};

# special Number formatting
my (@pctfields) = split(',',$configs->{'pctFields'});
my (@pxfields) = split(',',$configs->{'pxFields'});

# Kdb connect
my $k = Kx->new(name=>$ktable, host=>$khost, port=>$kport, userpass=>$kuserpass);
$k->connect($ktable);

##################Alog report########################################
my (@sections) = split(',', $sectionNames);
my $hcount = 0;

my (@filenames) = split(',',$ksqlfile);

# read multiple SQL files 
foreach my $ksql (@filenames) {

  my $section = $sections[$hcount++];
  # my $tableheader = "<h> <font face='Arial' size='-1' >" . $section .  "</font></h> <br></br>";
  my $tableheader = "<p><strong>" . $section .  "</strong></p>";

  print "xxxx section: $section:  sql file: $ksql \n";

  my $fh = new IO::File "< $curDir/$ksql";
  die "Error: Can't open $ksql for read: $!" if (!defined($fh));

# get the sql query
my $sqlstm = "";
while(my $line = $fh->getline) {
# replace $DATE 
#    $line =~ s/DATE/$(dateStr)/g;
    $sqlstm = join " ", $sqlstm, $line;
}
close $fh;

$sqlstm =~ s/DATE/$dateStr/g;
print "XXXX \n $sqlstm  \n";

$k->Tselect($ktable,"$sqlstm");
$k->Tget($ktable);  # Note: this must be called, or no meta data is returned from Kdb

my $header = $k->Theader($ktable);
print "@$header\n";

my $numrows = $k->Tnumrows($ktable);
my $numcols = $k->Tnumcols($ktable);
print "XXXX  $numrows $numcols \n";

# create report table
my $table .= '<table id="test1" border="1">';
$table .= '<tr BGCOLOR="#99CCFF">';
for (my $j=0; $j < $numcols; $j++)
{
    my $hd = @$header[$j];
    $table .= "<td>$hd</td>";
}
$table .= "</tr>";

my $market="";

for (my $i=0; $i < $numrows; $i++)
{
  my $row = $k->Trow($i);
  print "Roww $i data is: @$row[0] \n";

  # start an empty row if NEW market
#  my $newmarket = @$row[0];
#  if ($market ne $newmarket) {
#    $market = $newmarket;
#    $table .= "<tr>"; 
#    for (my $j=0; $j < $numcols; $j++) {
#      $table .= "<td> </td>";
#    }
#    $table .= "</tr>";
#  }


  if ($i%2 == 0)
  {
    $table .= "<tr>";
  }
  else 
  {
    $table .= '<tr BGCOLOR="#99CCFF">';
  }

  for (my $j=0; $j < $numcols; $j++)
  {
      my $cell = @$row[$j];
      if ( $cell eq "nan") {
	$cell = "";
      }

      # percentage formatting 
      foreach (@pctfields) {
        if ( $_ eq @$header[$j] ) { 
          $cell = $cell*100;
          $cell = sprintf("%.0f","$cell"); 
          $cell = sprintf("$cell%%",""); 
        }
      } 

      # dollar formatting 
      foreach (@pxfields) {
        if ( $_ eq @$header[$j] ) { 
          $cell = $de->format_price($cell,2,'$');	
        }
      } 

      if (@$header[$j] eq "Account" || @$header[$j] eq "ClientName") {
          $cell = sprintf("$cell",""); 
      }
      elsif ( @$header[$j] eq "BasketTime"  )
      {
      }
      elsif ( @$header[$j] =~ /Time/  )
      {
         # $cell = time2str("%H:%M:%S", $cell, 'America/New_York');
         $cell = time2str("%M:%S", $cell, 'America/New_York');
        #$cell = time2str("%X", $cell, 'America/New_York');
      }
      elsif ( @$header[$j] =~ /date/ || @$header[$j] =~ /Date/  )
      {
        $cell = time2str("%Y.%m.%d", $cell, 'America/New_York');
      }
      elsif (looks_like_number($cell))
      {
        $cell = $de->format_number($cell);	
      }

      $table .= "<td>$cell</td>";
  }
  $table .= "</tr>";
}
$table .= "</table>";
$table .="<BR/>";

$content .= $tableheader;
$content .= $table;
}

## complete html
$content .= "</body></HTML>";

print "\n xxx DONE. \n";

## email to:  
my $now = getTimeString();
my $fromEmail = $configs->{'fromEmail'};
my $replyTo = $configs->{'replyTo'};
my $ccEmail = $configs->{'ccGroup'};
my $email = $configs->{'emailGroup'};
my $subject = $configs->{'subject'};
$subject .= " ($now) ";
#$subject .= " ($khost:$kport) ";

##################Alog report########################################

$dateStr = getDateStr();
open(LOGS, ">$logDir/${configFile}.${dateStr}.log");
print LOGS "$content \n";
print LOGS "email to: $email \n";
close LOGS;

##sendEmail($fromEmail, $replyTo, $email, $ccEmail, $subject, $content);
sendEmailHtml($fromEmail, $replyTo, $email, $ccEmail, $subject, $content);
print "XXXX emailed to: $email \n";

exit 0;

#===========================================================
# common functions
#===========================================================

sub parseConf {
  my ($confFile) = @_;
print "Reading config file: $confFile \n";

  my $result;
  my $fh = new IO::File "< $confFile";
  die "Can't open $confFile for read: $!" if (!defined($fh));
  while(my $line = $fh->getline) {
    my ($key, $value) = parseLine($line);
    if(defined($key) && defined($value)) {
        $key =~ s/^\s+//; $value =~ s/^\s+//;
        $key =~ s/\s+$//; $value =~ s/\s+$//;
        $result->{$key} = $value;
    }
  }
  close $fh;
  return $result;
}

sub parseLine {
  my ($line) = @_;
  return (undef,undef) if ($line =~ m/^#/ or $line =~ m/^$/);

  my $result;
  chomp($line);
  $line =~ s/^\s+//;
  $line =~ s/\s+$//;
  my ($key, @values) = split(/=/, $line);
  return (undef, undef) if ($#values == -1);
  my $value = join("=", @values);
  return ($key, $value);
}

sub sendEmail {

    my $fromEmail = shift;
    my $replyTo = shift;
    my $email = shift;
    my $ccEmail = shift;
    my $subject = shift;

        my $msg = MIME::Lite->new(
                From            =>      $fromEmail,
		'Reply-to'      =>	$replyTo,
                To              =>      $email,
                Cc              =>      $ccEmail,
                Subject         =>      $subject,
                Type            =>      'text/html',
                Data            =>      $content,
        ) or die "Error creating message\n";

        ## my $mailServer = "mailer.rbc.com";
        my $mailServer = "mailsrv.ny.rbcds.com";
        MIME::Lite->send('smtp', $mailServer, Timeout=>60);

        $msg->send();
}

sub sendEmailHtml
{
    my( $from, $reply, $to, $cc, $subject, $content ) = @_;
    #$subject .= " - HTML";

    my %params = ();
    my %options = ();
    my $template = <<TEMPLATE;

$content

TEMPLATE

    my $msg = MIME::Lite::TT->new(
        From        => $from,
        To          => $to,
        Cc          => $cc,
        Subject     => $subject,
        Encoding    => 'quoted-printable',
        Type        => 'text/html',
        Template    => \$template,
        Charset     => 'utf8',
        TmplOptions => \%options,
        TmplParams  => \%params,
    );

    my $mailServer = "mailsrv.ny.rbcds.com";
    MIME::Lite->send('smtp', $mailServer, Timeout=>60);

    $msg->send;

    print " xxxx send HTML email to $to, $cc DONE. \n";
}





sub getTimeString {
  my $self = shift;
  my @timeData = localtime;
  my $hour = ${timeData[2]};
  my $minute = ${timeData[1]};
  my $second = ${timeData[0]};

  if($hour < 10) { $hour = "0${hour}"; }
  if($second < 10) { $second = "0${second}"; }
  if($minute < 10) { $minute = "0${minute}"; }

  #return "${timeData[2]}:${timeData[1]}:${timeData[0]}";
  return "$hour:$minute";
}


sub getDateStr {
  my @timeData = localtime;
  
  my $year = ${timeData[5]};
  my $mon = ${timeData[4]};
  my $mday = ${timeData[3]};
  
  $year = $year+1900;
  $mon = $mon+1;
  $mday = $mday;

  if ($mon <10) { $mon="0${mon}"; }
  if ($mday <10) { $mday="0${mday}"; }

  return "$year$mon$mday";
}

sub usage {
        print STDERR "\n";
        print STDERR "Usage: perl atdb_test.pl atdb.conf <must provide config file> \n";
        print STDERR "\n";
        exit 1;
}
