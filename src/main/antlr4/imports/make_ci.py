import sys


def make_insensitive(filename):
    if not filename.endswith('.g4.in'):
        raise ValueError('Unrecognized file-type for %s' % filename)

    # Chop off the ".in" part
    out_filename = filename[:-3]

    with open(filename) as inp, open(out_filename, 'w') as out:
        while True:
            line = inp.readline()
            if line == '':
                break

            i_comment = line.find("//")
            i_quote = line.find("'")

            if i_quote != -1 and (i_comment == -1 or i_quote < i_comment):
                i_endquote = line.find("'", i_quote+1)
                if i_endquote != -1:
                    text = line[i_quote+1:i_endquote]
                    text = text.upper()
                    insens = ['[%c%c]' % (ch, ch.lower()) for ch in text]
                    insens = ''.join(insens)

                    line = line[:i_quote] + insens + line[i_endquote+1:]

            out.write(line)


if len(sys.argv) == 1:
    print('usage: %s file1.g4.in [file2.g4.in ...]' % sys.argv[0])
    print('\tConverts the ANTLRv4 keyword file(s) to be case-insensitive.')
    print('\tRequires at least one file to be specified on the command-line.')
    sys.exit(1)

for filename in sys.argv[1:]:
    if not filename.endswith('.g4.in'):
        print('WARNING:  Unrecognized file-type for %s, skipping.' % filename)
        continue

    make_insensitive(filename)

