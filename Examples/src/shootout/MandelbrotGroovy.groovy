package shootout;
/* The Computer Language Benchmarks Game
   http://shootout.alioth.debian.org/

   contributed by Stefan Krause
   slightly modified by Chad Whipkey
   parallelized by Colin D Bennett 2008-10-04
   reduce synchronization cost by The Anh Tran
  */

import java.util.concurrent.atomic.AtomicInteger

@Typed
class MandelbrotGroovy
{
    public static void main(String[] args) throws Exception
    {
        int size = 200;
        if (args.length >= 1)
            size = Integer.parseInt(args[0]);

        System.out.format("P4\n%d %d\n", size, size);
        long millis = System.currentTimeMillis();
        int width_bytes = size /8 +1;
        byte[][] output_data = new byte[size][width_bytes];
        int[] bytes_per_line = new int[size];

        Compute(size, output_data, bytes_per_line);
        System.out.println("elapsed: "+ (System.currentTimeMillis() - millis));

        BufferedOutputStream ostream = new BufferedOutputStream(System.out);
        for (int i = 0; i < size; i++)
            ostream.write(output_data[i], 0, bytes_per_line[i]);
        ostream.close();
    }

    private static final void Compute(final int N, final byte[][] output, final int[] bytes_per_line)
    {
        final double inverse_N = 2.0 / N;
        final AtomicInteger current_line = new AtomicInteger(0);

        final Thread[] pool = new Thread[Runtime.getRuntime().availableProcessors()];
        for (int i = 0; i < pool.length; i++)
        {
            pool[i] = new Thread([
                "run": {
                    int y;
                    while ((y = current_line.getAndIncrement()) < N)
                    {
                        byte[] pdata = output[y];

                        int bit_num = 0;
                        int byte_count = 0;
                        int byte_accumulate = 0;

                        double Civ = (double)y * inverse_N - 1.0d;
                        for (int x = 0; x < N; x++)
                        {
                            double Crv = (double)x * inverse_N - 1.5d;

                            double Zrv = Crv;
                            double Ziv = Civ;

                            double Trv = Crv * Crv;
                            double Tiv = Civ * Civ;

                            int j = 49;
                            while (true)
                            {
                                Ziv = (Zrv * Ziv) + (Zrv * Ziv) + Civ;
                                Zrv = Trv - Tiv + Crv;

                                Trv = Zrv * Zrv;
                                Tiv = Ziv * Ziv;

                                if (((Trv + Tiv) > 4.0d) || (--j <= 0)) break
                            }

                            byte_accumulate <<= 1;
                            if (j == 0)
                                byte_accumulate++;

                            if (++bit_num == 8)
                            {
                                pdata[ byte_count++ ] = (byte)byte_accumulate;
                                bit_num = byte_accumulate = 0;
                            }
                        } // end foreach column

                        if (bit_num != 0)
                        {
                            byte_accumulate <<= (8 - (N & 7));
                            pdata[ byte_count++ ] = (byte)byte_accumulate;
                        }

                        bytes_per_line[y] = byte_count;
                    } // end while (y < N)
                } // end void run()
            ])

            pool[i].start();
        }

        for (Thread t : pool)
        {
            try
            {
                t.join();
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }
        }
    }
}